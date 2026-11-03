package cn.liuyang.desk.service.impl;

import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.converter.DeskConverter;
import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import cn.liuyang.desk.repository.Store;
import cn.liuyang.desk.repository.TicketRepository;
import cn.liuyang.desk.service.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketServiceImpl implements TicketService {
  private final TicketRepository repository;
  private final Store store;
  private final OrderService orders;
  private final DeskConverter converter;

  public TicketServiceImpl(
      TicketRepository repository, Store store, OrderService orders, DeskConverter converter) {
    this.repository = repository;
    this.store = store;
    this.orders = orders;
    this.converter = converter;
  }

  public List<TicketResponse> list(String actor, boolean staff) {
    List<Ticket> rows = staff ? repository.listAll() : repository.listOwned(actor);
    return rows.stream().map(converter::ticket).toList();
  }

  public TicketResponse get(String id, String actor, boolean staff) {
    Ticket ticket = staff ? repository.get(id) : repository.getOwned(id, actor);
    TicketResponse response = converter.ticket(ticket);
    List<TicketEvent> events = repository.events(id);
    response.setEvents(converter.events(events));
    return response;
  }

  @Transactional
  public TicketResponse create(
      String owner, String orderId, String kind, String subject, String description, String key) {
    if (!KINDS.contains(kind)) {
      throw new ApiException(400, "不支持的工单类型");
    }
    if (key == null || !key.matches("[A-Za-z0-9-]{8,70}")) {
      throw new ApiException(400, "缺少有效的重复提交标识");
    }
    repository.lockOwner(owner);
    String requestKey = owner + ":" + key;
    String hash =
        Store.hash(
            store.encode(List.of(orderId == null ? "" : orderId, kind, subject, description)));
    var existing = repository.findRequest(requestKey);
    if (!existing.isEmpty()) {
      if (!hash.equals(existing.getFirst().getRequestHash())) {
        throw new ApiException(409, "同一提交标识不能用于不同内容");
      }
      return get(existing.getFirst().getId(), owner, false);
    }
    String resolved = null;
    if (orderId != null && !orderId.isBlank()) {
      var order = orders.owned(owner, orderId);
      orders.eligible(order, kind);
      resolved = order.id();
    } else if (!"QUESTION".equals(kind)) {
      throw new ApiException(400, "售后申请必须关联本人订单");
    }
    String id = Store.id();
    String now = Store.now();
    String number = "TK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    repository.insertTicket(
        id, number, owner, resolved, kind, subject, description, requestKey, hash, now);
    event(id, owner, "CREATED", description);
    store.audit(owner, "TICKET_CREATE", id, "创建" + kind + "工单");
    return get(id, owner, false);
  }

  @Transactional
  public TicketResponse update(
      String id, String actor, boolean staff, int version, String status, String message) {
    var ticket = repository.lockTicket(id);
    String currentStatus = ticket.getStatus();
    String nextStatus =
        cn.liuyang.desk.domain.TicketPolicy.nextStatus(
            ticket.getOwnerId(),
            ticket.getVersion(),
            currentStatus,
            actor,
            staff,
            version,
            status,
            message);
    String assignee = staff && ticket.getAssigneeId() == null ? actor : ticket.getAssigneeId();
    repository.updateState(nextStatus, assignee, id);
    if (message != null && !message.isBlank()) {
      event(id, actor, "REPLY", message.trim());
    }
    if (!nextStatus.equals(currentStatus)) {
      event(id, actor, "STATUS", currentStatus + " → " + nextStatus);
    }
    store.audit(actor, "TICKET_UPDATE", id, currentStatus + " → " + nextStatus);
    return get(id, actor, staff);
  }

  @Transactional
  public Proposal propose(
      String owner, String orderId, String kind, String subject, String description) {
    if (!KINDS.contains(kind)) {
      kind = "QUESTION";
    }
    String resolved = null;
    if (orderId != null && !orderId.isBlank()) {
      var order = orders.owned(owner, orderId);
      orders.eligible(order, kind);
      resolved = order.id();
    } else if (!"QUESTION".equals(kind)) {
      throw new ApiException(400, "请提供本人订单号以生成售后草稿");
    }
    String id = Store.id();
    repository.insertProposal(
        id,
        owner,
        resolved,
        kind,
        subject,
        description,
        Instant.now().plus(15, ChronoUnit.MINUTES).toString());
    return repository.getProposal(id);
  }

  @Transactional
  public TicketResponse confirm(String id, String owner) {
    // Same owner lock order as direct creation; then lock this proposal.
    repository.lockOwner(owner);
    var proposal = repository.lockProposal(id, owner);
    if (proposal.getTicketId() != null) {
      return get(proposal.getTicketId(), owner, false);
    }
    if (Instant.parse(proposal.getExpiresAt()).isBefore(Instant.now())) {
      throw new ApiException(410, "草稿已过期，请重新生成");
    }
    var ticket =
        create(
            owner,
            proposal.getOrderId(),
            proposal.getKind(),
            proposal.getSubject(),
            proposal.getDescription(),
            id);
    repository.confirmProposal(ticket.getId(), id);
    return ticket;
  }

  private void event(String ticket, String actor, String type, String content) {
    repository.insertEvent(ticket, actor, type, content);
  }
}
