package cn.liuyang.desk.service;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import java.util.*;

/** Business contract; see service.impl.TicketServiceImpl for execution. */
public interface TicketService {
  java.util.Set<String> KINDS = java.util.Set.of("REFUND", "EXCHANGE", "REPAIR", "QUESTION");

  List<TicketResponse> list(String actor, boolean staff);

  TicketResponse get(String id, String actor, boolean staff);

  TicketResponse create(
      String owner, String orderId, String kind, String subject, String description, String key);

  TicketResponse update(
      String id, String actor, boolean staff, int version, String status, String message);

  Proposal propose(String owner, String orderId, String kind, String subject, String description);

  TicketResponse confirm(String id, String owner);
}
