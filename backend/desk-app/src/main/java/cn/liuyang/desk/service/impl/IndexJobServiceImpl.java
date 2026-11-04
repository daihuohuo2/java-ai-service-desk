package cn.liuyang.desk.service.impl;

import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.dto.IndexJob;
import cn.liuyang.desk.repository.IndexJobRepository;
import cn.liuyang.desk.repository.Store;
import cn.liuyang.desk.service.*;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The database row is both the durable job record and the outbox to be delivered. */
@Service
public class IndexJobServiceImpl implements IndexJobService {
  private final IndexJobRepository repository;
  private final Store store;

  public IndexJobServiceImpl(IndexJobRepository repository, Store store) {
    this.repository = repository;
    this.store = store;
  }

  @Transactional
  public IndexJob submit(String documentId, String actor, String key) {
    if (key == null || !key.matches("[A-Za-z0-9-]{8,70}")) {
      throw new ApiException(400, "缺少有效的重复提交标识");
    }
    documentId = documentId == null || documentId.isBlank() ? null : documentId;
    repository.lockOwner(actor);
    var existing = repository.findRequest(actor + ":" + key);
    if (!existing.isEmpty()) {
      if (!Objects.equals(existing.getFirst().documentId(), documentId)) {
        throw new ApiException(409, "同一提交标识不能用于不同资料");
      }
      return existing.getFirst();
    }
    if (documentId != null) {
      repository.requireActiveDocument(documentId);
    }
    if (repository.pendingCount() >= 20) {
      throw new ApiException(429, "待处理任务较多，请稍后再试");
    }
    String id = Store.id();
    repository.insert(id, documentId, actor, actor + ":" + key);
    store.audit(actor, "INDEX_JOB_CREATE", id, "提交知识索引任务");
    return get(id);
  }

  public List<IndexJob> list() {
    return repository.list();
  }

  public IndexJob get(String id) {
    return repository.find(id).stream().findFirst().orElseThrow(ApiException::missing);
  }

  @Transactional
  public IndexJob retry(String id, String actor) {
    repository.lock(id);
    if (repository.retry(id) != 1) {
      throw new ApiException(409, "只有失败的任务可以手动重试");
    }
    store.audit(actor, "INDEX_JOB_RETRY", id, "重新提交索引任务");
    return get(id);
  }

  public List<String> ready(int limit) {
    long now = System.currentTimeMillis();
    return repository.ready(now, limit);
  }
}
