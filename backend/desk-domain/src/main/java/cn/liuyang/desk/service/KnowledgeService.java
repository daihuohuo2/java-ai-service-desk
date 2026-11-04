package cn.liuyang.desk.service;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import java.util.*;

/** Business contract; see service.impl.KnowledgeServiceImpl for execution. */
public interface KnowledgeService {
  List<KnowledgeDocument> list();

  KnowledgeDocument add(String title, String product, String content, String actor);

  ActiveResponse setActive(String id, boolean active, String actor);

  IndexResult index(String id, String actor);

  IndexResult index(String id, String actor, Runnable heartbeat);

  List<KnowledgeHit> search(String question);
}
