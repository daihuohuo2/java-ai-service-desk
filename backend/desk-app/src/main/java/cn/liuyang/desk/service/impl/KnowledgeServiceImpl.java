package cn.liuyang.desk.service.impl;

import static cn.liuyang.desk.domain.KnowledgeText.*;

import cn.liuyang.desk.ai.AiGateway;
import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import cn.liuyang.desk.repository.KnowledgeRepository;
import cn.liuyang.desk.repository.Store;
import cn.liuyang.desk.service.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {
  private final KnowledgeRepository repository;
  private final Store store;
  private final AiGateway ai;

  public KnowledgeServiceImpl(KnowledgeRepository repository, Store store, AiGateway ai) {
    this.repository = repository;
    this.store = store;
    this.ai = ai;
  }

  public List<KnowledgeDocument> list() {
    return repository.documents();
  }

  @Transactional
  public KnowledgeDocument add(String title, String product, String content, String actor) {
    if (content == null || content.isBlank() || content.length() > 30000) {
      throw new ApiException(400, "资料须为 1 至 30000 个字符");
    }
    var chunks = split(content);
    repository.lockCatalog();
    int total = repository.activeChunkCount();
    if (total + chunks.size() > 200) {
      throw new ApiException(409, "当前版本最多索引 200 个有效片段，请先停用旧资料");
    }
    if (product != null && !product.isBlank()) {
      repository.requireProduct(product);
    } else product = null;
    String id = Store.id();
    repository.insertDocument(id, title, product);
    int index = 0;
    for (String chunk : chunks) repository.insertChunk(id, ++index, chunk);
    store.audit(actor, "KNOWLEDGE_CREATE", id, "上传资料，分段数 " + chunks.size());
    return repository.document(id);
  }

  @Transactional
  public ActiveResponse setActive(String id, boolean active, String actor) {
    repository.lockCatalog();
    repository.requireDocument(id);
    if (active) {
      int total = repository.activatedChunkCount(id);
      if (total > 200) {
        throw new ApiException(409, "超过 200 个有效片段上限");
      }
    }
    repository.setActive(active, id);
    store.audit(actor, "KNOWLEDGE_STATE", id, active ? "启用资料" : "停用资料");
    return new ActiveResponse(active);
  }

  public IndexResult index(String id, String actor) {
    return index(id, actor, () -> {});
  }

  public IndexResult index(String id, String actor, Runnable heartbeat) {
    var rows = id == null ? repository.missingVectors() : repository.documentChunks(id);
    int indexed = 0;
    for (var row : rows) {
      heartbeat.run();
      float[] vector = ai.embed(row.getContent());
      if (vector == null) {
        break;
      }
      heartbeat.run();
      repository.saveVector(store.encode(vector), ai.embeddingName(), row.getId());
      indexed++;
    }
    store.audit(actor, "KNOWLEDGE_INDEX", id, "向量化 " + indexed + " / " + rows.size() + " 个片段");
    return new IndexResult(indexed, rows.size(), indexed == rows.size());
  }

  public List<KnowledgeHit> search(String question) {
    var products = repository.productNames();
    String product = null;
    for (var p : products) {
      String name = p.getName();
      if (question.contains(name)
          || question.contains(p.getId())
          || (name.length() >= 3 && question.contains(name.substring(name.length() - 3)))) {
        product = p.getId();
      }
    }
    var all = repository.searchableChunks();
    boolean hasVector =
        all.stream()
            .anyMatch(
                x -> x.getEmbedding() != null && ai.embeddingName().equals(x.getEmbeddingModel()));
    float[] query = hasVector ? ai.embed(question) : null;
    var ranked = new ArrayList<KnowledgeHit>();
    for (var chunk : all) {
      if (product != null
          && chunk.getProductId() != null
          && !product.equals(chunk.getProductId())) {
        continue;
      }
      String content = chunk.getContent();
      double lexical = lexical(question, content + chunk.getTitle());
      double vector = 0;
      if (query != null
          && chunk.getEmbedding() != null
          && ai.embeddingName().equals(chunk.getEmbeddingModel())) {
        try {
          vector = cosine(query, store.json.readValue(chunk.getEmbedding(), float[].class));
        } catch (Exception ignored) {
        }
      }
      if (lexical < 0.025 && vector < 0.55) {
        continue;
      }
      var hit =
          new KnowledgeHit(
              chunk.getId(),
              chunk.getTitle(),
              chunk.getVersion(),
              chunk.getOrdinal(),
              content,
              Math.round((lexical + vector * 0.6) * 1000) / 1000.0,
              query == null ? "keyword" : "hybrid");
      ranked.add(hit);
    }
    ranked.sort(Comparator.comparingDouble(x -> -x.score()));
    return ranked.stream().limit(5).toList();
  }
}
