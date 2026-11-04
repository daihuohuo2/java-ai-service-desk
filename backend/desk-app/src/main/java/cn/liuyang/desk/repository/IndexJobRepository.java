package cn.liuyang.desk.repository;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Typed SQL boundary. Transactions are owned by the calling business service. */
@Repository
@RequiredArgsConstructor
public class IndexJobRepository {
  private final Store store;
  private static final RowMapper<IndexJob> MAPPER =
      (r, n) ->
          new IndexJob(
              r.getString("id"),
              r.getString("document_id"),
              r.getString("state"),
              r.getInt("attempts"),
              r.getInt("indexed"),
              r.getInt("total"),
              r.getString("error_message"),
              r.getString("created_at"),
              r.getString("updated_at"));

  public void lockOwner(String actor) {
    store.require("SELECT id FROM app_users WHERE id=? FOR UPDATE", actor);
  }

  public List<IndexJob> findRequest(String requestKey) {
    return store.db.query("SELECT * FROM index_jobs WHERE request_key=?", MAPPER, requestKey);
  }

  public void requireActiveDocument(String documentId) {
    store.require("SELECT id FROM knowledge_documents WHERE id=? AND active=true", documentId);
  }

  public Integer pendingCount() {
    return store.db.queryForObject(
        "SELECT COUNT(*) FROM index_jobs WHERE state IN ('PENDING','RUNNING','RETRY')",
        Integer.class);
  }

  public int insert(String id, String documentId, String actor, String requestKey) {
    return store.db.update(
        "INSERT INTO"
            + " index_jobs(id,document_id,actor_id,request_key,state,available_at,created_at,updated_at)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        id,
        documentId,
        actor,
        requestKey,
        "PENDING",
        System.currentTimeMillis(),
        Store.now(),
        Store.now());
  }

  public List<IndexJob> list() {
    return store.db.query("SELECT * FROM index_jobs ORDER BY created_at DESC,id LIMIT 50", MAPPER);
  }

  public List<IndexJob> find(String id) {
    return store.db.query("SELECT * FROM index_jobs WHERE id=?", MAPPER, id);
  }

  public void lock(String id) {
    store.require("SELECT id FROM index_jobs WHERE id=? FOR UPDATE", id);
  }

  public int retry(String id) {
    return store.db.update(
        "UPDATE index_jobs SET"
            + " state='PENDING',attempts=0,available_at=?,lease_until=0,error_message=NULL,updated_at=?"
            + " WHERE id=? AND state='FAILED'",
        System.currentTimeMillis(),
        Store.now(),
        id);
  }

  public List<String> ready(long now, int limit) {
    return store.db.queryForList(
        "SELECT id FROM index_jobs WHERE (state IN ('PENDING','RETRY') AND available_at<=?) OR"
            + " (state='RUNNING' AND lease_until<?) ORDER BY created_at,id LIMIT ?",
        String.class,
        now,
        now,
        limit);
  }
}
