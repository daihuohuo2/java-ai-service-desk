package cn.liuyang.desk;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketQueryService {
    private final Store store;
    private static final Set<String> STATES = Set.of("OPEN", "IN_PROGRESS", "WAITING_CUSTOMER", "RESOLVED", "CLOSED");
    private static final String FROM = " FROM tickets t JOIN app_users u ON t.owner_id=u.id ";
    private static final String COLUMNS = "t.id,t.ticket_no,t.subject,t.kind,t.status,t.updated_at,u.display_name customer_name";

    public TicketQueryService(Store store) { this.store = store; }

    record Filter(String sql, List<Object> args) { }

    private Filter filter(String owner, boolean staff, String status, String query) {
        var sql = new StringBuilder(" WHERE 1=1");
        var args = new ArrayList<Object>();
        if (!staff) { sql.append(" AND t.owner_id=?"); args.add(owner); }
        if (status != null && !status.isBlank()) {
            if (!STATES.contains(status)) throw new ApiException(400, "不支持的工单状态");
            sql.append(" AND t.status=?"); args.add(status);
        }
        if (query != null && query.length() > 120) throw new ApiException(400, "关键词最多 120 个字符");
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            sql.append(" AND (LOWER(t.subject) LIKE ? ESCAPE '!' OR LOWER(t.ticket_no) LIKE ? ESCAPE '!')");
            args.add(pattern); args.add(pattern);
        }
        return new Filter(sql.toString(), args);
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public PageResponse<Map<String,Object>> page(String owner, boolean staff, int page, int size, String status, String query) {
        PageResponse.validate(page, size);
        Filter filter = filter(owner, staff, status, query);
        long total = store.db.queryForObject("SELECT COUNT(*)" + FROM + filter.sql(), Long.class, filter.args().toArray());
        var args = new ArrayList<>(filter.args());
        args.add(size); args.add((long) (page - 1) * size);
        var rows = store.db.queryForList("SELECT " + COLUMNS + FROM + filter.sql()
                + " ORDER BY t.updated_at DESC,t.id LIMIT ? OFFSET ?", args.toArray());
        return PageResponse.of(rows, total, page, size);
    }

    public String export(String owner, boolean staff, String status, String query) {
        Filter filter = filter(owner, staff, status, query);
        var rows = store.db.queryForList("SELECT " + COLUMNS + FROM + filter.sql()
                + " ORDER BY t.updated_at DESC,t.id LIMIT 1001", filter.args().toArray());
        if (rows.size() > 1000) throw new ApiException(400, "结果超过 1000 条，请缩小筛选范围后导出");
        var lines = new ArrayList<List<?>>();
        lines.add(List.of("工单编号", "标题", "类型", "状态", "客户", "更新时间"));
        for (var row : rows) lines.add(List.of(row.get("ticket_no"), row.get("subject"), row.get("kind"),
                row.get("status"), row.get("customer_name"), row.get("updated_at")));
        return CsvExport.encode(lines);
    }
}
