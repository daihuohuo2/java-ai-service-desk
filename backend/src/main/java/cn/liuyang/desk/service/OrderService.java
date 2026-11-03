package cn.liuyang.desk.service;

import cn.liuyang.desk.common.PageResponse;
import cn.liuyang.desk.dto.OrderResponse;
import java.util.List;

/** Business contract; see service.impl.OrderServiceImpl for execution. */
public interface OrderService {
  List<OrderResponse> list(String owner);

  PageResponse<OrderResponse> page(String owner, int page, int size, String status, String query);

  OrderResponse owned(String owner, String id);

  void eligible(OrderResponse order, String kind);
}
