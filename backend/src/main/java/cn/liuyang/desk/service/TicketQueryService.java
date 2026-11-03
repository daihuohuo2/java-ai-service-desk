package cn.liuyang.desk.service;

import cn.liuyang.desk.common.PageResponse;
import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import java.util.*;

/** Business contract; see service.impl.TicketQueryServiceImpl for execution. */
public interface TicketQueryService {
  PageResponse<TicketSummary> page(
      String owner, boolean staff, int page, int size, String status, String query);

  String export(String owner, boolean staff, String status, String query);
}
