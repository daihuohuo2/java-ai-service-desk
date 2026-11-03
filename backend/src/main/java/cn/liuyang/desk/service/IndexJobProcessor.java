package cn.liuyang.desk.service;

import cn.liuyang.desk.repository.Store;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

/**
 * At-least-once delivery: claim with SQL CAS, heartbeat the lease, fence completions by lease
 * token.
 */
@Service
public class IndexJobProcessor {
  private final Store store;
  private final KnowledgeService knowledge;
  private final MeterRegistry metrics;
  static final long LEASE_MS = 240000;

  public IndexJobProcessor(Store store, KnowledgeService knowledge, MeterRegistry metrics) {
    this.store = store;
    this.knowledge = knowledge;
    this.metrics = metrics;
  }

  public void process(String id) {
    long now = System.currentTimeMillis();
    String token = Store.id();
    int claimed =
        store.db.update(
            "UPDATE index_jobs SET"
                + " state='RUNNING',attempts=attempts+1,lease_until=?,lease_token=?,updated_at=?"
                + " WHERE id=? AND ((state IN ('PENDING','RETRY') AND available_at<=?) OR"
                + " (state='RUNNING' AND lease_until<?))",
            now + LEASE_MS,
            token,
            Store.now(),
            id,
            now,
            now);
    if (claimed == 0) return; // Another consumer owns it, or it has already finished.
    var job = store.one("SELECT * FROM index_jobs WHERE id=?", id);
    int attempt = ((Number) job.get("attempts")).intValue();
    if (attempt > IndexJobService.MAX_ATTEMPTS) {
      finishFailure(id, attempt, token);
      return;
    }
    try {
      var result =
          knowledge.index(
              (String) job.get("document_id"),
              (String) job.get("actor_id"),
              () -> {
                int held =
                    store.db.update(
                        "UPDATE index_jobs SET lease_until=?,updated_at=? WHERE id=? AND"
                            + " state='RUNNING' AND lease_token=?",
                        System.currentTimeMillis() + LEASE_MS,
                        Store.now(),
                        id,
                        token);
                if (held != 1) throw new IllegalStateException("Index job lease lost");
              });
      int indexed = ((Number) result.get("indexed")).intValue();
      int total = ((Number) result.get("total")).intValue();
      store.db.update(
          "UPDATE index_jobs SET indexed=?,total=?,updated_at=? WHERE id=? AND state='RUNNING' AND"
              + " lease_token=?",
          indexed,
          total,
          Store.now(),
          id,
          token);
      if (!Boolean.TRUE.equals(result.get("complete")))
        throw new IllegalStateException("Embedding unavailable");
      int finished =
          store.db.update(
              "UPDATE index_jobs SET"
                  + " state='SUCCEEDED',error_message=NULL,lease_until=0,updated_at=? WHERE id=?"
                  + " AND state='RUNNING' AND lease_token=?",
              Store.now(),
              id,
              token);
      if (finished == 1) metrics.counter("desk.index.jobs", "result", "success").increment();
    } catch (RuntimeException exception) {
      finishFailure(id, attempt, token);
    }
  }

  private void finishFailure(String id, int attempt, String token) {
    String state = attempt >= IndexJobService.MAX_ATTEMPTS ? "FAILED" : "RETRY";
    int changed =
        store.db.update(
            "UPDATE index_jobs SET"
                + " state=?,available_at=?,lease_until=0,error_message=?,updated_at=? WHERE id=?"
                + " AND state='RUNNING' AND lease_token=?",
            state,
            System.currentTimeMillis() + 5000L * Math.min(attempt, 3),
            "索引未完成，请检查模型服务；可在恢复后重试",
            Store.now(),
            id,
            token);
    if (changed == 1)
      metrics
          .counter("desk.index.jobs", "result", state.toLowerCase(java.util.Locale.ROOT))
          .increment();
  }
}
