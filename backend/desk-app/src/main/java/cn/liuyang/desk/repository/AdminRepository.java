package cn.liuyang.desk.repository;

import cn.liuyang.desk.dto.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminRepository {
  private final Store store;

  public OverviewResponse overview() {
    List<TicketCount> counts =
        store.db.query(
            "SELECT status,COUNT(*) total FROM tickets GROUP BY status",
            (row, index) -> new TicketCount(row.getString("status"), row.getLong("total")));
    return new OverviewResponse(
        counts,
        store.db.queryForObject("SELECT COUNT(*) FROM shop_orders", Integer.class),
        store.db.queryForObject(
            "SELECT COUNT(*) FROM knowledge_documents WHERE active=true", Integer.class),
        store.db.queryForObject("SELECT COUNT(*) FROM chat_runs", Integer.class),
        store.list(
            "SELECT id,state,model_mode,latency_ms,created_at FROM chat_runs ORDER BY created_at"
                + " DESC LIMIT 8",
            RunSummary.class));
  }

  public List<AuditResponse> audit() {
    return store.list(
        "SELECT a.*,u.display_name actor_name FROM audit_log a LEFT JOIN app_users u ON"
            + " a.actor_id=u.id ORDER BY created_at DESC LIMIT 100",
        AuditResponse.class);
  }
}
