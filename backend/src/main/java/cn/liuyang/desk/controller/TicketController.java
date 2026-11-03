package cn.liuyang.desk.controller;

import cn.liuyang.desk.dto.TicketResponse;
import cn.liuyang.desk.security.Actor;
import cn.liuyang.desk.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TicketController {
  private final Actor actor;
  private final TicketService tickets;

  public TicketController(Actor actor, TicketService tickets) {
    this.actor = actor;
    this.tickets = tickets;
  }

  @GetMapping("/tickets")
  java.util.List<TicketResponse> tickets() {
    return tickets.list(actor.id(), actor.staff());
  }

  @GetMapping("/tickets/{id}")
  TicketResponse ticket(@PathVariable String id) {
    return tickets.get(id, actor.id(), actor.staff());
  }

  record NewTicket(
      @Size(max = 40) String orderId,
      @NotBlank String kind,
      @NotBlank @Size(max = 120) String subject,
      @NotBlank @Size(max = 2000) String description) {}

  @PostMapping("/tickets")
  TicketResponse create(
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @Valid @RequestBody NewTicket request) {
    return tickets.create(
        actor.id(),
        request.orderId(),
        request.kind(),
        request.subject().trim(),
        request.description().trim(),
        key);
  }

  record TicketUpdate(
      @Min(0) int version, @Size(max = 30) String status, @Size(max = 2000) String message) {}

  @PostMapping("/tickets/{id}/events")
  TicketResponse update(@PathVariable String id, @Valid @RequestBody TicketUpdate request) {
    return tickets.update(
        id, actor.id(), actor.staff(), request.version(), request.status(), request.message());
  }

  @PostMapping("/proposals/{id}/confirm")
  TicketResponse confirm(@PathVariable String id) {
    return tickets.confirm(id, actor.id());
  }
}
