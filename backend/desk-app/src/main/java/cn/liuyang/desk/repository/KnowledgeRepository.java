package cn.liuyang.desk.repository;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Typed SQL boundary. Transactions are owned by the calling business service. */
@Repository
@RequiredArgsConstructor
public class KnowledgeRepository {
  private final Store store;

  public List<KnowledgeDocument> documents() {
    return store.list(
        "SELECT d.*,COUNT(c.id) chunks,SUM(CASE WHEN c.embedding IS NOT NULL THEN 1 ELSE 0 END)"
            + " indexed FROM knowledge_documents d LEFT JOIN knowledge_chunks c ON"
            + " d.id=c.document_id GROUP BY"
            + " d.id,d.title,d.product_id,d.version,d.active,d.created_at ORDER BY d.created_at"
            + " DESC",
        KnowledgeDocument.class);
  }

  public void lockCatalog() {
    store.require("SELECT id FROM app_users WHERE id='admin' FOR UPDATE");
  }

  public Integer activeChunkCount() {
    return store.db.queryForObject(
        "SELECT COUNT(*) FROM knowledge_chunks c JOIN knowledge_documents d ON"
            + " c.document_id=d.id WHERE d.active=true",
        Integer.class);
  }

  public void requireProduct(String product) {
    store.require("SELECT id FROM products WHERE id=?", product);
  }

  public int insertDocument(String id, String title, String product) {
    return store.db.update(
        "INSERT INTO knowledge_documents VALUES(?,?,?,?,?,?)",
        id,
        title,
        product,
        1,
        true,
        Store.now());
  }

  public int insertChunk(String id, int ordinal, String chunk) {
    return store.db.update(
        "INSERT INTO knowledge_chunks VALUES(?,?,?,?,?,?)",
        Store.id(),
        id,
        ordinal,
        chunk,
        null,
        null);
  }

  public KnowledgeDocument document(String id) {
    return store.one("SELECT * FROM knowledge_documents WHERE id=?", KnowledgeDocument.class, id);
  }

  public void requireDocument(String id) {
    store.require("SELECT id FROM knowledge_documents WHERE id=?", id);
  }

  public Integer activatedChunkCount(String id) {
    return store.db.queryForObject(
        "SELECT COUNT(*) FROM knowledge_chunks c JOIN knowledge_documents d ON"
            + " c.document_id=d.id WHERE d.active=true OR d.id=?",
        Integer.class,
        id);
  }

  public int setActive(boolean active, String id) {
    return store.db.update("UPDATE knowledge_documents SET active=? WHERE id=?", active, id);
  }

  public List<KnowledgeChunk> missingVectors() {
    return store.list(
        "SELECT c.* FROM knowledge_chunks c JOIN knowledge_documents d ON"
            + " c.document_id=d.id WHERE d.active=true AND c.embedding IS NULL ORDER BY"
            + " d.id,c.ordinal",
        KnowledgeChunk.class);
  }

  public List<KnowledgeChunk> documentChunks(String id) {
    return store.list(
        "SELECT c.* FROM knowledge_chunks c JOIN knowledge_documents d ON"
            + " c.document_id=d.id WHERE d.id=? AND d.active=true ORDER BY c.ordinal",
        KnowledgeChunk.class,
        id);
  }

  public int saveVector(String embedding, String embeddingModel, String chunkId) {
    return store.db.update(
        "UPDATE knowledge_chunks SET embedding=?,embedding_model=? WHERE id=?",
        embedding,
        embeddingModel,
        chunkId);
  }

  public List<Product> productNames() {
    return store.list("SELECT id,name FROM products", Product.class);
  }

  public List<KnowledgeChunk> searchableChunks() {
    return store.list(
        "SELECT c.*,d.title,d.version,d.product_id FROM knowledge_chunks c JOIN"
            + " knowledge_documents d ON c.document_id=d.id WHERE d.active=true ORDER BY"
            + " d.id,c.ordinal",
        KnowledgeChunk.class);
  }
}
