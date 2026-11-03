package cn.liuyang.desk.controller;

import cn.liuyang.desk.common.PageResponse;
import cn.liuyang.desk.security.Actor;
import cn.liuyang.desk.service.TicketQueryService;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketQueryController {
  private final TicketQueryService queries;
  private final Actor actor;

  public TicketQueryController(TicketQueryService queries, Actor actor) {
    this.queries = queries;
    this.actor = actor;
  }

  @GetMapping("/page")
  public PageResponse<Map<String, Object>> page(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String query) {
    return queries.page(actor.id(), actor.staff(), page, size, status, query);
  }

  @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
  public ResponseEntity<String> export(
      @RequestParam(required = false) String status, @RequestParam(required = false) String query) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets.csv\"")
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(queries.export(actor.id(), actor.staff(), status, query));
  }
}
