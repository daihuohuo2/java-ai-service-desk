package cn.liuyang.desk;

import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The database row is both the durable job record and the outbox to be delivered. */
@Service
public class IndexJobService {
    private final Store store;
    static final int MAX_ATTEMPTS = 3;
    private static final RowMapper<IndexJob> MAPPER = (r, n) -> new IndexJob(
            r.getString("id"), r.getString("document_id"), r.getString("state"),
            r.getInt("attempts"), r.getInt("indexed"), r.getInt("total"),
            r.getString("error_message"), r.getString("created_at"), r.getString("updated_at"));

    public IndexJobService(Store store) { this.store = store; }

    @Transactional
    public IndexJob submit(String documentId, String actor, String key) {
        if (key == null || !key.matches("[A-Za-z0-9-]{8,70}"))
            throw new ApiException(400, "缺少有效的重复提交标识");
        documentId = documentId == null || documentId.isBlank() ? null : documentId;
        store.one("SELECT id FROM app_users WHERE id=? FOR UPDATE", actor);
        var existing = store.db.query("SELECT * FROM index_jobs WHERE request_key=?", MAPPER, actor + ":" + key);
        if (!existing.isEmpty()) {
            if (!Objects.equals(existing.getFirst().documentId(), documentId))
                throw new ApiException(409, "同一提交标识不能用于不同资料");
            return existing.getFirst();
        }
        if (documentId != null)
            store.one("SELECT id FROM knowledge_documents WHERE id=? AND active=true", documentId);
        if (store.db.queryForObject("SELECT COUNT(*) FROM index_jobs WHERE state IN ('PENDING','RUNNING','RETRY')", Integer.class) >= 20)
            throw new ApiException(429, "待处理任务较多，请稍后再试");
        String id = Store.id();
        store.db.update("INSERT INTO index_jobs(id,document_id,actor_id,request_key,state,available_at,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?)",
                id, documentId, actor, actor + ":" + key, "PENDING", System.currentTimeMillis(), Store.now(), Store.now());
        store.audit(actor, "INDEX_JOB_CREATE", id, "提交知识索引任务");
        return get(id);
    }

    public List<IndexJob> list() {
        return store.db.query("SELECT * FROM index_jobs ORDER BY created_at DESC,id LIMIT 50", MAPPER);
    }

    public IndexJob get(String id) {
        return store.db.query("SELECT * FROM index_jobs WHERE id=?", MAPPER, id).stream()
                .findFirst().orElseThrow(ApiException::missing);
    }

    @Transactional
    public IndexJob retry(String id, String actor) {
        store.one("SELECT id FROM index_jobs WHERE id=? FOR UPDATE", id);
        if (store.db.update("UPDATE index_jobs SET state='PENDING',attempts=0,available_at=?,lease_until=0,error_message=NULL,updated_at=? WHERE id=? AND state='FAILED'",
                System.currentTimeMillis(), Store.now(), id) != 1)
            throw new ApiException(409, "只有失败的任务可以手动重试");
        store.audit(actor, "INDEX_JOB_RETRY", id, "重新提交索引任务");
        return get(id);
    }

    public List<String> ready(int limit) {
        long now = System.currentTimeMillis();
        return store.db.queryForList("SELECT id FROM index_jobs WHERE (state IN ('PENDING','RETRY') AND available_at<=?) OR (state='RUNNING' AND lease_until<?) ORDER BY created_at,id LIMIT ?",
                String.class, now, now, limit);
    }
}
