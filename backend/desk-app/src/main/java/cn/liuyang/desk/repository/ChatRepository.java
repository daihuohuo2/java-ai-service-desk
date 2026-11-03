package cn.liuyang.desk.repository;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Typed SQL boundary. Transactions are owned by the calling business service. */
@Repository
@RequiredArgsConstructor
public class ChatRepository {
  private final Store store;

  public List<ChatThread> threads(String owner) {
    return store.list(
        "SELECT * FROM chat_threads WHERE owner_id=? ORDER BY created_at DESC LIMIT 50",
        ChatThread.class,
        owner);
  }

  public int insertThread(String id, String owner) {
    return store.db.update(
        "INSERT INTO chat_threads VALUES(?,?,?,?)", id, owner, "新的咨询", Store.now());
  }

  public ChatThread thread(String id) {
    return store.one("SELECT * FROM chat_threads WHERE id=?", ChatThread.class, id);
  }

  public void requireOwner(String id, String owner) {
    store.require("SELECT id FROM chat_threads WHERE id=? AND owner_id=?", id, owner);
  }

  public List<ChatMessage> messages(String id) {
    return store.list(
        "SELECT * FROM chat_messages WHERE thread_id=? ORDER BY created_at,id",
        ChatMessage.class,
        id);
  }

  public List<ChatRun> findRun(String requestKey) {
    return store.list("SELECT * FROM chat_runs WHERE request_key=?", ChatRun.class, requestKey);
  }

  public int insertRun(
      String runId, String owner, String thread, String requestKey, String question) {
    return store.db.update(
        "INSERT INTO chat_runs(id,owner_id,thread_id,request_key,question,state,created_at)"
            + " VALUES(?,?,?,?,?,?,?)",
        runId,
        owner,
        thread,
        requestKey,
        question,
        "RUNNING",
        Store.now());
  }

  public int insertQuestion(String thread, String question) {
    return store.db.update(
        "INSERT INTO chat_messages VALUES(?,?,?,?,?,?)",
        Store.id(),
        thread,
        "user",
        question,
        null,
        Store.now());
  }

  public int setInitialTitle(String title, String thread) {
    return store.db.update(
        "UPDATE chat_threads SET title=? WHERE id=? AND title='新的咨询'", title, thread);
  }

  public List<String> history(String thread) {
    return store.db.queryForList(
        "SELECT content FROM chat_messages WHERE thread_id=? AND role='user' ORDER BY created_at"
            + " DESC LIMIT 4",
        String.class,
        thread);
  }

  public int insertAnswer(String thread, String answer, String detailJson) {
    return store.db.update(
        "INSERT INTO chat_messages VALUES(?,?,?,?,?,?)",
        Store.id(),
        thread,
        "assistant",
        answer,
        detailJson,
        Store.now());
  }

  public int completeRun(String detailJson, String mode, long latencyMs, String runId) {
    return store.db.update(
        "UPDATE chat_runs SET state='COMPLETED',result_json=?,model_mode=?,latency_ms=? WHERE"
            + " id=?",
        detailJson,
        mode,
        latencyMs,
        runId);
  }

  public int failRun(long latencyMs, String runId) {
    return store.db.update(
        "UPDATE chat_runs SET state='FAILED',latency_ms=? WHERE id=?", latencyMs, runId);
  }
}
