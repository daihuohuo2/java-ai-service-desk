package cn.liuyang.desk.service;

import cn.liuyang.desk.ai.AiGateway;
import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.repository.Store;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
  private final Store s;
  private final AiGateway ai;
  private final KnowledgeService knowledge;
  private final OrderService orders;
  private final TicketService tickets;
  private final Set<String> active = ConcurrentHashMap.newKeySet();

  public ChatService(
      Store s,
      AiGateway ai,
      KnowledgeService knowledge,
      OrderService orders,
      TicketService tickets) {
    this.s = s;
    this.ai = ai;
    this.knowledge = knowledge;
    this.orders = orders;
    this.tickets = tickets;
  }

  public Object threads(String owner) {
    return s.db.queryForList(
        "SELECT * FROM chat_threads WHERE owner_id=? ORDER BY created_at DESC LIMIT 50", owner);
  }

  public Map<String, Object> newThread(String owner) {
    String id = Store.id();
    s.db.update("INSERT INTO chat_threads VALUES(?,?,?,?)", id, owner, "新的咨询", Store.now());
    return s.one("SELECT * FROM chat_threads WHERE id=?", id);
  }

  public void ownThread(String owner, String id) {
    s.one("SELECT id FROM chat_threads WHERE id=? AND owner_id=?", id, owner);
  }

  public List<Map<String, Object>> messages(String owner, String id) {
    ownThread(owner, id);
    var rows =
        s.db.queryForList(
            "SELECT * FROM chat_messages WHERE thread_id=? ORDER BY created_at,id", id);
    for (var row : rows)
      if (row.get("detail_json") != null)
        row.put("detail", s.decode((String) row.remove("detail_json")));
    return rows;
  }

  public Map<String, Object> answer(String owner, String thread, String question, String key) {
    ownThread(owner, thread);
    if (key == null || !key.matches("[A-Za-z0-9-]{8,70}")) throw new ApiException(400, "缺少有效的请求标识");
    if (!active.add(owner)) throw new ApiException(429, "上一条咨询仍在处理中，请稍候");
    long started = System.currentTimeMillis();
    String runId = Store.id();
    boolean inserted = false;
    try {
      var existing =
          s.db.queryForList("SELECT * FROM chat_runs WHERE request_key=?", owner + ":" + key);
      if (!existing.isEmpty()) {
        var run = existing.getFirst();
        if (!thread.equals(run.get("thread_id")) || !question.equals(run.get("question")))
          throw new ApiException(409, "同一请求标识不能用于不同问题");
        if (run.get("result_json") != null) return s.decode((String) run.get("result_json"));
        throw new ApiException(409, "该请求尚无确定结果，请查看会话记录后重新提问");
      }
      s.db.update(
          "INSERT INTO chat_runs(id,owner_id,thread_id,request_key,question,state,created_at)"
              + " VALUES(?,?,?,?,?,?,?)",
          runId,
          owner,
          thread,
          owner + ":" + key,
          question,
          "RUNNING",
          Store.now());
      inserted = true;
      s.db.update(
          "INSERT INTO chat_messages VALUES(?,?,?,?,?,?)",
          Store.id(),
          thread,
          "user",
          question,
          null,
          Store.now());
      s.db.update(
          "UPDATE chat_threads SET title=? WHERE id=? AND title='新的咨询'",
          question.substring(0, Math.min(24, question.length())),
          thread);
      var history =
          s
              .db
              .queryForList(
                  "SELECT content FROM chat_messages WHERE thread_id=? AND role='user' ORDER BY"
                      + " created_at DESC LIMIT 4",
                  thread)
              .stream()
              .map(x -> (String) x.get("content"))
              .toList();
      var sources = knowledge.search(question);
      var plan = ai.plan(question, history, sources);
      var result = new LinkedHashMap<String, Object>();
      result.put("runId", runId);
      result.put("mode", plan.mode());
      result.put("promptVersion", AiGateway.PROMPT_VERSION);
      result.put("intent", plan.intent());
      result.put("citations", List.of());
      switch (plan.intent()) {
        case "ORDER" -> {
          try {
            var found =
                plan.orderNumber().isBlank()
                    ? orders.list(owner)
                    : List.of(orders.owned(owner, plan.orderNumber()));
            result.put("orders", found);
            result.put("tool", "orders.lookupOwned");
            result.put(
                "answer", found.isEmpty() ? "暂时没有查询到您的订单。" : "已从业务系统查询到您本人的订单。以下金额、商品和状态来自订单记录。");
          } catch (ApiException e) {
            result.put("answer", "未查询到您本人的对应订单，请核对订单号。您也可以在“我的订单”中选择订单。");
          }
        }
        case "AFTER_SALES", "HUMAN" -> {
          if (!"OLLAMA".equals(plan.mode())) throw new ApiException(503, "模型不可用，请使用手动创建工单");
          String kind = "HUMAN".equals(plan.intent()) ? "QUESTION" : plan.kind();
          try {
            var proposal =
                tickets.propose(
                    owner,
                    plan.orderNumber(),
                    kind,
                    "QUESTION".equals(kind) ? "人工咨询申请" : "售后处理申请",
                    question);
            result.put("proposal", proposal);
            result.put("tool", "tickets.prepareDraft");
            result.put("answer", "已整理一份待确认的工单草稿。请核对内容后点击“确认创建工单”；此时还没有创建工单，也未批准退款。草稿 15 分钟内有效。");
          } catch (ApiException e) {
            result.put(
                "answer", e.status == 404 ? "未查询到您本人的对应订单，请在“我的订单”中选择订单后申请售后。" : e.getMessage());
          }
        }
        default -> {
          var chosen = selectSources(sources, plan.sourceIds(), !"OLLAMA".equals(plan.mode()));
          result.put("citations", chosen);
          result.put("tool", "knowledge.search");
          if (chosen.isEmpty())
            result.put("answer", "现有资料不足以确认这个问题。我不会推测未提供的产品参数或售后承诺。您可以补充商品名称，或创建人工咨询工单。");
          else {
            StringBuilder answer = new StringBuilder("根据目前已发布的资料：\n\n");
            for (int i = 0; i < chosen.size(); i++)
              answer
                  .append("[")
                  .append(i + 1)
                  .append("] ")
                  .append(chosen.get(i).get("content"))
                  .append("\n\n");
            result.put("answer", answer.toString().trim());
          }
        }
      }
      result.put("latencyMs", System.currentTimeMillis() - started);
      s.db.update(
          "INSERT INTO chat_messages VALUES(?,?,?,?,?,?)",
          Store.id(),
          thread,
          "assistant",
          result.get("answer"),
          s.encode(result),
          Store.now());
      s.db.update(
          "UPDATE chat_runs SET state='COMPLETED',result_json=?,model_mode=?,latency_ms=? WHERE"
              + " id=?",
          s.encode(result),
          plan.mode(),
          result.get("latencyMs"),
          runId);
      s.audit(owner, "CHAT_COMPLETE", runId, plan.intent() + " / " + plan.mode());
      return result;
    } catch (RuntimeException e) {
      if (inserted)
        s.db.update(
            "UPDATE chat_runs SET state='FAILED',latency_ms=? WHERE id=?",
            System.currentTimeMillis() - started,
            runId);
      throw e;
    } finally {
      active.remove(owner);
    }
  }

  public static List<Map<String, Object>> selectSources(
      List<Map<String, Object>> candidates, List<String> ids, boolean fallback) {
    if (fallback)
      return candidates.stream()
          .filter(x -> ((Number) x.get("score")).doubleValue() >= 0.12)
          .limit(2)
          .toList();
    Set<String> known = new HashSet<>();
    candidates.forEach(x -> known.add((String) x.get("id")));
    if (ids.stream().anyMatch(x -> !known.contains(x))) return List.of();
    return candidates.stream().filter(x -> ids.contains(x.get("id"))).limit(3).toList();
  }
}
