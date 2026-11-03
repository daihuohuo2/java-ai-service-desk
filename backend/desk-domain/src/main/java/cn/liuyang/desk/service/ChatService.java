package cn.liuyang.desk.service;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import java.util.*;

/** Business contract; see service.impl.ChatServiceImpl for execution. */
public interface ChatService {
  List<ChatThread> threads(String owner);

  ChatThread newThread(String owner);

  void ownThread(String owner, String id);

  List<ChatMessageResponse> messages(String owner, String id);

  ChatAnswer answer(String owner, String thread, String question, String key);
}
