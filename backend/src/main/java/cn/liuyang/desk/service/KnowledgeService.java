package cn.liuyang.desk.service;

import cn.liuyang.desk.ai.AiGateway;
import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.repository.Store;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeService {
  private final Store s;
  private final AiGateway ai;

  public KnowledgeService(Store s, AiGateway ai) {
    this.s = s;
    this.ai = ai;
  }

  public List<Map<String, Object>> list() {
    return s.db.queryForList(
        "SELECT d.*,COUNT(c.id) chunks,SUM(CASE WHEN c.embedding IS NOT NULL THEN 1 ELSE 0 END)"
            + " indexed FROM knowledge_documents d LEFT JOIN knowledge_chunks c ON"
            + " d.id=c.document_id GROUP BY"
            + " d.id,d.title,d.product_id,d.version,d.active,d.created_at ORDER BY d.created_at"
            + " DESC");
  }

  @Transactional
  public Object add(String title, String product, String content, String actor) {
    if (content == null || content.isBlank() || content.length() > 30000)
      throw new ApiException(400, "资料须为 1 至 30000 个字符");
    var chunks = split(content);
    s.one("SELECT id FROM app_users WHERE id='admin' FOR UPDATE");
    int total =
        s.db.queryForObject(
            "SELECT COUNT(*) FROM knowledge_chunks c JOIN knowledge_documents d ON"
                + " c.document_id=d.id WHERE d.active=true",
            Integer.class);
    if (total + chunks.size() > 200) throw new ApiException(409, "当前版本最多索引 200 个有效片段，请先停用旧资料");
    if (product != null && !product.isBlank()) s.one("SELECT id FROM products WHERE id=?", product);
    else product = null;
    String id = Store.id();
    s.db.update(
        "INSERT INTO knowledge_documents VALUES(?,?,?,?,?,?)",
        id,
        title,
        product,
        1,
        true,
        Store.now());
    int index = 0;
    for (String chunk : chunks)
      s.db.update(
          "INSERT INTO knowledge_chunks VALUES(?,?,?,?,?,?)",
          Store.id(),
          id,
          ++index,
          chunk,
          null,
          null);
    s.audit(actor, "KNOWLEDGE_CREATE", id, "上传资料，分段数 " + chunks.size());
    return s.one("SELECT * FROM knowledge_documents WHERE id=?", id);
  }

  @Transactional
  public Object setActive(String id, boolean active, String actor) {
    s.one("SELECT id FROM app_users WHERE id='admin' FOR UPDATE");
    s.one("SELECT id FROM knowledge_documents WHERE id=?", id);
    if (active) {
      int total =
          s.db.queryForObject(
              "SELECT COUNT(*) FROM knowledge_chunks c JOIN knowledge_documents d ON"
                  + " c.document_id=d.id WHERE d.active=true OR d.id=?",
              Integer.class,
              id);
      if (total > 200) throw new ApiException(409, "超过 200 个有效片段上限");
    }
    s.db.update("UPDATE knowledge_documents SET active=? WHERE id=?", active, id);
    s.audit(actor, "KNOWLEDGE_STATE", id, active ? "启用资料" : "停用资料");
    return Map.of("active", active);
  }

  public Map<String, Object> index(String id, String actor) {
    return index(id, actor, () -> {});
  }

  public Map<String, Object> index(String id, String actor, Runnable heartbeat) {
    var rows =
        id == null
            ? s.db.queryForList(
                "SELECT c.* FROM knowledge_chunks c JOIN knowledge_documents d ON"
                    + " c.document_id=d.id WHERE d.active=true AND c.embedding IS NULL ORDER BY"
                    + " d.id,c.ordinal")
            : s.db.queryForList(
                "SELECT c.* FROM knowledge_chunks c JOIN knowledge_documents d ON"
                    + " c.document_id=d.id WHERE d.id=? AND d.active=true ORDER BY c.ordinal",
                id);
    int indexed = 0;
    for (var row : rows) {
      heartbeat.run();
      float[] vector = ai.embed((String) row.get("content"));
      if (vector == null) break;
      heartbeat.run();
      s.db.update(
          "UPDATE knowledge_chunks SET embedding=?,embedding_model=? WHERE id=?",
          s.encode(vector),
          ai.embeddingName(),
          row.get("id"));
      indexed++;
    }
    s.audit(actor, "KNOWLEDGE_INDEX", id, "向量化 " + indexed + " / " + rows.size() + " 个片段");
    return Map.of("indexed", indexed, "total", rows.size(), "complete", indexed == rows.size());
  }

  public List<Map<String, Object>> search(String question) {
    var products = s.db.queryForList("SELECT id,name FROM products");
    String product = null;
    for (var p : products) {
      String name = (String) p.get("name");
      if (question.contains(name)
          || question.contains((String) p.get("id"))
          || (name.length() >= 3 && question.contains(name.substring(name.length() - 3))))
        product = (String) p.get("id");
    }
    var all =
        s.db.queryForList(
            "SELECT c.*,d.title,d.version,d.product_id FROM knowledge_chunks c JOIN"
                + " knowledge_documents d ON c.document_id=d.id WHERE d.active=true ORDER BY"
                + " d.id,c.ordinal");
    boolean hasVector =
        all.stream()
            .anyMatch(
                x ->
                    x.get("embedding") != null
                        && ai.embeddingName().equals(x.get("embedding_model")));
    float[] query = hasVector ? ai.embed(question) : null;
    var ranked = new ArrayList<Map<String, Object>>();
    for (var chunk : all) {
      if (product != null
          && chunk.get("product_id") != null
          && !product.equals(chunk.get("product_id"))) continue;
      String content = (String) chunk.get("content");
      double lexical = lexical(question, content + chunk.get("title"));
      double vector = 0;
      if (query != null
          && chunk.get("embedding") != null
          && ai.embeddingName().equals(chunk.get("embedding_model"))) {
        try {
          vector = cosine(query, s.json.readValue((String) chunk.get("embedding"), float[].class));
        } catch (Exception ignored) {
        }
      }
      if (lexical < 0.025 && vector < 0.55) continue;
      var hit = new LinkedHashMap<String, Object>();
      hit.put("id", chunk.get("id"));
      hit.put("title", chunk.get("title"));
      hit.put("version", chunk.get("version"));
      hit.put("ordinal", chunk.get("ordinal"));
      hit.put("content", content);
      hit.put("score", Math.round((lexical + vector * 0.6) * 1000) / 1000.0);
      hit.put("retrieval", query == null ? "keyword" : "hybrid");
      ranked.add(hit);
    }
    ranked.sort(Comparator.comparingDouble(x -> -((Number) x.get("score")).doubleValue()));
    return ranked.stream().limit(5).toList();
  }

  static List<String> split(String input) {
    var result = new ArrayList<String>();
    for (String part : input.replace("\r\n", "\n").split("\n\\s*\n")) {
      part = part.strip();
      for (int i = 0; i < part.length(); i += 600)
        result.add(part.substring(i, Math.min(i + 600, part.length())));
    }
    if (result.size() > 60) throw new ApiException(400, "单份资料最多 60 个片段");
    return result;
  }

  static Set<String> grams(String s) {
    s = s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    var result = new HashSet<String>();
    for (int i = 0; i < s.length() - 1; i++) result.add(s.substring(i, i + 2));
    return result;
  }

  static double lexical(String a, String b) {
    var x = grams(a);
    var y = grams(b);
    if (x.isEmpty()) return 0;
    long intersection = x.stream().filter(y::contains).count();
    return (double) intersection / x.size();
  }

  static double cosine(float[] a, float[] b) {
    if (a.length != b.length) return 0;
    double sum = 0, na = 0, nb = 0;
    for (int i = 0; i < a.length; i++) {
      sum += a[i] * b[i];
      na += a[i] * a[i];
      nb += b[i] * b[i];
    }
    return na == 0 || nb == 0 ? 0 : sum / Math.sqrt(na * nb);
  }
}
