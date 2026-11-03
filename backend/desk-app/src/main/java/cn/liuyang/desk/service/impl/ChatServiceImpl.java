package cn.liuyang.desk.service.impl;

import static cn.liuyang.desk.domain.CitationPolicy.selectSources;

import cn.liuyang.desk.ai.AiGateway;
import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import cn.liuyang.desk.repository.ChatRepository;
import cn.liuyang.desk.repository.Store;
import cn.liuyang.desk.service.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements ChatService {
  private final ChatRepository repository;
  private final Store store;
  private final AiGateway ai;
  private final KnowledgeService knowledge;
  private final OrderService orders;
  private final TicketService tickets;
  private final cn.liuyang.desk.converter.DeskConverter converter;
  private final Set<String> active = ConcurrentHashMap.newKeySet();

  public ChatServiceImpl(
      ChatRepository repository,
      Store store,
      AiGateway ai,
      KnowledgeService knowledge,
      OrderService orders,
      TicketService tickets,
      cn.liuyang.desk.converter.DeskConverter converter) {
    this.repository = repository;
    this.store = store;
    this.ai = ai;
    this.knowledge = knowledge;
    this.orders = orders;
    this.tickets = tickets;
    this.converter = converter;
  }

  public List<ChatThread> threads(String owner) {
    return repository.threads(owner);
  }

  public ChatThread newThread(String owner) {
    String id = Store.id();
    repository.insertThread(id, owner);
    return repository.thread(id);
  }

  public void ownThread(String owner, String id) {
    repository.requireOwner(id, owner);
  }

  public List<ChatMessageResponse> messages(String owner, String id) {
    ownThread(owner, id);
    return repository.messages(id).stream()
        .map(
            message -> {
              ChatMessageResponse response = converter.message(message);
              if (message.getDetailJson() != null) {
                response.setDetail(store.decode(message.getDetailJson(), ChatAnswer.class));
              }
              return response;
            })
        .toList();
  }

  public ChatAnswer answer(String owner, String thread, String question, String key) {
    ownThread(owner, thread);
    if (key == null || !key.matches("[A-Za-z0-9-]{8,70}")) {
      throw new ApiException(400, "缺少有效的请求标识");
    }
    if (!active.add(owner)) {
      throw new ApiException(429, "上一条咨询仍在处理中，请稍候");
    }
    long started = System.currentTimeMillis();
    String runId = Store.id();
    boolean inserted = false;
    try {
      var existing = repository.findRun(owner + ":" + key);
      if (!existing.isEmpty()) {
        var run = existing.getFirst();
        if (!thread.equals(run.getThreadId()) || !question.equals(run.getQuestion())) {
          throw new ApiException(409, "同一请求标识不能用于不同问题");
        }
        if (run.getResultJson() != null) {
          return store.decode(run.getResultJson(), ChatAnswer.class);
        }
        throw new ApiException(409, "该请求尚无确定结果，请查看会话记录后重新提问");
      }
      repository.insertRun(runId, owner, thread, owner + ":" + key, question);
      inserted = true;
      repository.insertQuestion(thread, question);
      repository.setInitialTitle(question.substring(0, Math.min(24, question.length())), thread);
      List<String> history = repository.history(thread);
      var sources = knowledge.search(question);
      var plan = ai.plan(question, history, sources);
      var result = new ChatAnswer();
      result.setRunId(runId);
      result.setMode(plan.mode());
      result.setPromptVersion(AiGateway.PROMPT_VERSION);
      result.setIntent(plan.intent());
      result.setCitations(List.of());
      switch (plan.intent()) {
        case "ORDER" -> {
          try {
            var found =
                plan.orderNumber().isBlank()
                    ? orders.list(owner)
                    : List.of(orders.owned(owner, plan.orderNumber()));
            result.setOrders(found);
            result.setTool("orders.lookupOwned");
            result.setAnswer(
                found.isEmpty() ? "暂时没有查询到您的订单。" : "已从业务系统查询到您本人的订单。以下金额、商品和状态来自订单记录。");
          } catch (ApiException e) {
            result.setAnswer("未查询到您本人的对应订单，请核对订单号。您也可以在“我的订单”中选择订单。");
          }
        }
        case "AFTER_SALES", "HUMAN" -> {
          if (!"OLLAMA".equals(plan.mode())) {
            throw new ApiException(503, "模型不可用，请使用手动创建工单");
          }
          String kind = "HUMAN".equals(plan.intent()) ? "QUESTION" : plan.kind();
          try {
            var proposal =
                tickets.propose(
                    owner,
                    plan.orderNumber(),
                    kind,
                    "QUESTION".equals(kind) ? "人工咨询申请" : "售后处理申请",
                    question);
            result.setProposal(proposal);
            result.setTool("tickets.prepareDraft");
            result.setAnswer("已整理一份待确认的工单草稿。请核对内容后点击“确认创建工单”；此时还没有创建工单，也未批准退款。草稿 15 分钟内有效。");
          } catch (ApiException e) {
            result.setAnswer(e.status == 404 ? "未查询到您本人的对应订单，请在“我的订单”中选择订单后申请售后。" : e.getMessage());
          }
        }
        default -> {
          var chosen = selectSources(sources, plan.sourceIds(), !"OLLAMA".equals(plan.mode()));
          result.setCitations(chosen);
          result.setTool("knowledge.search");
          if (chosen.isEmpty()) {
            result.setAnswer("现有资料不足以确认这个问题。我不会推测未提供的产品参数或售后承诺。您可以补充商品名称，或创建人工咨询工单。");
          } else {
            StringBuilder answer = new StringBuilder("根据目前已发布的资料：\n\n");
            for (int i = 0; i < chosen.size(); i++)
              answer
                  .append("[")
                  .append(i + 1)
                  .append("] ")
                  .append(chosen.get(i).content())
                  .append("\n\n");
            result.setAnswer(answer.toString().trim());
          }
        }
      }
      result.setLatencyMs(System.currentTimeMillis() - started);
      repository.insertAnswer(thread, result.getAnswer(), store.encode(result));
      repository.completeRun(store.encode(result), plan.mode(), result.getLatencyMs(), runId);
      store.audit(owner, "CHAT_COMPLETE", runId, plan.intent() + " / " + plan.mode());
      return result;
    } catch (RuntimeException e) {
      if (inserted) {
        repository.failRun(System.currentTimeMillis() - started, runId);
      }
      throw e;
    } finally {
      active.remove(owner);
    }
  }
}
