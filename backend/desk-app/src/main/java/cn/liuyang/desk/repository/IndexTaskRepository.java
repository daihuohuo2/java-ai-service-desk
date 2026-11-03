package cn.liuyang.desk.repository;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Typed SQL boundary. Transactions are owned by the calling business service. */
@Repository
@RequiredArgsConstructor
public class IndexTaskRepository {
  private final Store store;

  public int claim(long leaseUntil, String token, String id, long now) {
    return store.db.update(
        "UPDATE index_jobs SET"
            + " state='RUNNING',attempts=attempts+1,lease_until=?,lease_token=?,updated_at=?"
            + " WHERE id=? AND ((state IN ('PENDING','RETRY') AND available_at<=?) OR"
            + " (state='RUNNING' AND lease_until<?))",
        leaseUntil,
        token,
        Store.now(),
        id,
        now,
        now);
  }

  public IndexTask getTask(String id) {
    return store.one("SELECT * FROM index_jobs WHERE id=?", IndexTask.class, id);
  }

  public int renew(long leaseUntil, String id, String token) {
    return store.db.update(
        "UPDATE index_jobs SET lease_until=?,updated_at=? WHERE id=? AND"
            + " state='RUNNING' AND lease_token=?",
        leaseUntil,
        Store.now(),
        id,
        token);
  }

  public int progress(int indexed, int total, String id, String token) {
    return store.db.update(
        "UPDATE index_jobs SET indexed=?,total=?,updated_at=? WHERE id=? AND state='RUNNING' AND"
            + " lease_token=?",
        indexed,
        total,
        Store.now(),
        id,
        token);
  }

  public int succeed(String id, String token) {
    return store.db.update(
        "UPDATE index_jobs SET"
            + " state='SUCCEEDED',error_message=NULL,lease_until=0,updated_at=? WHERE id=?"
            + " AND state='RUNNING' AND lease_token=?",
        Store.now(),
        id,
        token);
  }

  public int fail(String state, long availableAt, String id, String token) {
    return store.db.update(
        "UPDATE index_jobs SET"
            + " state=?,available_at=?,lease_until=0,error_message=?,updated_at=? WHERE id=?"
            + " AND state='RUNNING' AND lease_token=?",
        state,
        availableAt,
        "索引未完成，请检查模型服务；可在恢复后重试",
        Store.now(),
        id,
        token);
  }
}
