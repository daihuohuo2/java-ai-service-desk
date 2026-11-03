package cn.liuyang.desk.service.impl;

import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.common.PageResponse;
import cn.liuyang.desk.dto.OrderResponse;
import cn.liuyang.desk.mapper.OrderMapper;
import cn.liuyang.desk.service.*;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

  private final OrderMapper orders;
  private static final Set<String> STATUSES = Set.of("PAID", "SHIPPED", "DELIVERED", "CANCELLED");

  public OrderServiceImpl(OrderMapper orders) {
    this.orders = orders;
  }

  public List<OrderResponse> list(String owner) {
    return orders.list(owner);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PageResponse<OrderResponse> page(
      String owner, int page, int size, String status, String query) {
    PageResponse.validate(page, size);
    status = status == null || status.isBlank() ? null : status;
    if (status != null && !STATUSES.contains(status)) {
      throw new ApiException(400, "不支持的订单状态");
    }
    if (query != null && query.length() > 40) {
      throw new ApiException(400, "订单号关键词最多 40 个字符");
    }
    String pattern =
        query == null || query.isBlank()
            ? null
            : "%" + query.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    long total = orders.count(owner, status, pattern);
    var rows = orders.page(owner, status, pattern, size, (long) (page - 1) * size);
    return PageResponse.of(rows, total, page, size);
  }

  public OrderResponse owned(String owner, String id) {
    OrderResponse order = orders.owned(owner, id);
    if (order == null) {
      throw ApiException.missing();
    }
    return order;
  }

  public void eligible(OrderResponse order, String kind) {
    if (!"QUESTION".equals(kind) && !"DELIVERED".equals(order.status())) {
      throw new ApiException(409, "该订单尚未签收，可先提交咨询工单");
    }
  }
}
