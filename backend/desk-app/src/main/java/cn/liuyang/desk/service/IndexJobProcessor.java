package cn.liuyang.desk.service;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import cn.liuyang.desk.repository.IndexTaskRepository;
import cn.liuyang.desk.repository.Store;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

/**
 * At-least-once delivery: claim with SQL CAS, heartbeat the lease, fence completions by lease
 * token.
 */
@Service
public class IndexJobProcessor {
  private final IndexTaskRepository repository;
  private final Store store;
  private final KnowledgeService knowledge;
  private final MeterRegistry metrics;
  static final long LEASE_MS = 240000;

  public IndexJobProcessor(
      IndexTaskRepository repository,
      Store store,
      KnowledgeService knowledge,
      MeterRegistry metrics) {
    this.repository = repository;
    this.store = store;
    this.knowledge = knowledge;
    this.metrics = metrics;
  }

  public void process(String id) {
    long now = System.currentTimeMillis();
    String token = Store.id();
    int claimed = repository.claim(now + LEASE_MS, token, id, now);
    if (claimed == 0) {
      return;
    } // Another consumer owns it, or it has already finished.
    var job = repository.getTask(id);
    int attempt = job.getAttempts();
    if (attempt > IndexJobService.MAX_ATTEMPTS) {
      finishFailure(id, attempt, token);
      return;
    }
    try {
      var result =
          knowledge.index(
              job.getDocumentId(),
              job.getActorId(),
              () -> {
                int held = repository.renew(System.currentTimeMillis() + LEASE_MS, id, token);
                if (held != 1) {
                  throw new IllegalStateException("Index job lease lost");
                }
              });
      int indexed = result.indexed();
      int total = result.total();
      repository.progress(indexed, total, id, token);
      if (!result.complete()) {
        throw new IllegalStateException("Embedding unavailable");
      }
      int finished = repository.succeed(id, token);
      if (finished == 1) {
        metrics.counter("desk.index.jobs", "result", "success").increment();
      }
    } catch (RuntimeException exception) {
      finishFailure(id, attempt, token);
    }
  }

  private void finishFailure(String id, int attempt, String token) {
    String state = attempt >= IndexJobService.MAX_ATTEMPTS ? "FAILED" : "RETRY";
    int changed =
        repository.fail(
            state, System.currentTimeMillis() + 5000L * Math.min(attempt, 3), id, token);
    if (changed == 1) {
      metrics
          .counter("desk.index.jobs", "result", state.toLowerCase(java.util.Locale.ROOT))
          .increment();
    }
  }
}
