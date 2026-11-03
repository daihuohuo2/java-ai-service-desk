package cn.liuyang.desk.controller;

import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.security.Actor;
import cn.liuyang.desk.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
  private final Actor actor;
  private final ChatService chats;
  private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();

  public ChatController(Actor actor, ChatService chats) {
    this.actor = actor;
    this.chats = chats;
  }

  @GetMapping("/threads")
  Object threads() {
    return chats.threads(actor.id());
  }

  @PostMapping("/threads")
  Object create() {
    return chats.newThread(actor.id());
  }

  @GetMapping("/threads/{id}/messages")
  Object messages(@PathVariable String id) {
    return chats.messages(actor.id(), id);
  }

  record Question(
      @NotBlank @Size(max = 40) String threadId, @NotBlank @Size(max = 1000) String question) {}

  @PostMapping("/ask")
  Object ask(
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @Valid @RequestBody Question q) {
    return chats.answer(actor.id(), q.threadId(), q.question().trim(), key);
  }

  @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter stream(
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @Valid @RequestBody Question q) {
    String owner = actor.id();
    chats.ownThread(owner, q.threadId());
    var emitter = new SseEmitter(145000L);
    workers.submit(
        () -> {
          try {
            emitter.send(
                SseEmitter.event().name("phase").data(Map.of("message", "正在检索资料并核对业务请求…")));
            var result = chats.answer(owner, q.threadId(), q.question().trim(), key);
            emitter.send(SseEmitter.event().name("result").data(result));
            emitter.send(SseEmitter.event().name("done").data(Map.of("ok", true)));
            emitter.complete();
          } catch (Exception e) {
            try {
              emitter.send(
                  SseEmitter.event()
                      .name("error")
                      .data(
                          Map.of(
                              "error",
                              e instanceof ApiException ? e.getMessage() : "咨询中断，请查看会话记录后重试")));
              emitter.complete();
            } catch (Exception ignored) {
              emitter.completeWithError(e);
            }
          }
        });
    return emitter;
  }
}
