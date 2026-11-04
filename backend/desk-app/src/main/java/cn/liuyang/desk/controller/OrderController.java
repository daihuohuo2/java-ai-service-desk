package cn.liuyang.desk.controller;

import cn.liuyang.desk.common.PageResponse;
import cn.liuyang.desk.dto.OrderResponse;
import cn.liuyang.desk.security.Actor;
import cn.liuyang.desk.service.OrderService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService orders;
  private final Actor actor;

  public OrderController(OrderService orders, Actor actor) {
    this.orders = orders;
    this.actor = actor;
  }

  @GetMapping
  public List<OrderResponse> list() {
    return orders.list(actor.id());
  }

  @GetMapping("/page")
  public PageResponse<OrderResponse> page(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String query) {
    return orders.page(actor.id(), page, size, status, query);
  }

  @GetMapping("/{id}")
  public OrderResponse get(@PathVariable String id) {
    return orders.owned(actor.id(), id);
  }
}
