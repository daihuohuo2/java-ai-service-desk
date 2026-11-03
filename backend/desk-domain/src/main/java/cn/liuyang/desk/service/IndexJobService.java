package cn.liuyang.desk.service;

import cn.liuyang.desk.dto.IndexJob;
import java.util.List;

/** Business contract; see service.impl.IndexJobServiceImpl for execution. */
public interface IndexJobService {
  int MAX_ATTEMPTS = 3;

  IndexJob submit(String documentId, String actor, String key);

  List<IndexJob> list();

  IndexJob get(String id);

  IndexJob retry(String id, String actor);

  List<String> ready(int limit);
}
