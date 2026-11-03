package cn.liuyang.desk.repository;

import cn.liuyang.desk.dto.*;
import cn.liuyang.desk.entity.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Typed SQL boundary. Transactions are owned by the calling business service. */
@Repository
@RequiredArgsConstructor
public class TicketRepository {
  private final Store store;
  private static final String SELECT =
      "SELECT t.*,u.display_name customer_name,p.name product_name,o.order_no,a.display_name"
          + " assignee_name FROM tickets t JOIN app_users u ON t.owner_id=u.id LEFT JOIN"
          + " shop_orders o ON t.order_id=o.id LEFT JOIN products p ON o.product_id=p.id LEFT JOIN"
          + " app_users a ON t.assignee_id=a.id ";

  public List<Ticket> listAll() {
    return store.list(SELECT + "ORDER BY t.updated_at DESC LIMIT 200", Ticket.class);
  }

  public List<Ticket> listOwned(String actor) {
    return store.list(
        SELECT + "WHERE t.owner_id=? ORDER BY t.updated_at DESC LIMIT 200", Ticket.class, actor);
  }

  public Ticket get(String id) {
    return store.one(SELECT + "WHERE t.id=?", Ticket.class, id);
  }

  public Ticket getOwned(String id, String actor) {
    return store.one(SELECT + "WHERE t.id=? AND t.owner_id=?", Ticket.class, id, actor);
  }

  public List<TicketEvent> events(String id) {
    return store.list(
        "SELECT e.*,u.display_name actor_name,u.role actor_role FROM ticket_events e JOIN app_users"
            + " u ON e.actor_id=u.id WHERE e.ticket_id=? ORDER BY e.created_at,e.id",
        TicketEvent.class,
        id);
  }

  public void lockOwner(String owner) {
    store.require("SELECT id FROM app_users WHERE id=? FOR UPDATE", owner);
  }

  public List<Ticket> findRequest(String requestKey) {
    return store.list(
        "SELECT id,request_hash FROM tickets WHERE request_key=?", Ticket.class, requestKey);
  }

  public int insertTicket(
      String id,
      String number,
      String owner,
      String resolved,
      String kind,
      String subject,
      String description,
      String requestKey,
      String hash,
      String now) {
    return store.db.update(
        "INSERT INTO"
            + " tickets(id,ticket_no,owner_id,order_id,kind,subject,description,status,request_key,request_hash,created_at,updated_at)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
        id,
        number,
        owner,
        resolved,
        kind,
        subject,
        description,
        "OPEN",
        requestKey,
        hash,
        now,
        now);
  }

  public Ticket lockTicket(String id) {
    return store.one("SELECT * FROM tickets WHERE id=? FOR UPDATE", Ticket.class, id);
  }

  public int updateState(String next, String assignee, String id) {
    return store.db.update(
        "UPDATE tickets SET status=?,assignee_id=?,version=version+1,updated_at=? WHERE id=?",
        next,
        assignee,
        Store.now(),
        id);
  }

  public int insertProposal(
      String id,
      String owner,
      String resolved,
      String kind,
      String subject,
      String description,
      String expiresAt) {
    return store.db.update(
        "INSERT INTO proposals VALUES(?,?,?,?,?,?,?,?,?)",
        id,
        owner,
        resolved,
        kind,
        subject,
        description,
        expiresAt,
        null,
        Store.now());
  }

  public Proposal getProposal(String id) {
    return store.one("SELECT * FROM proposals WHERE id=?", Proposal.class, id);
  }

  public Proposal lockProposal(String id, String owner) {
    return store.one(
        "SELECT * FROM proposals WHERE id=? AND owner_id=? FOR UPDATE", Proposal.class, id, owner);
  }

  public int confirmProposal(String ticketId, String id) {
    return store.db.update("UPDATE proposals SET ticket_id=? WHERE id=?", ticketId, id);
  }

  public int insertEvent(String ticket, String actor, String type, String content) {
    return store.db.update(
        "INSERT INTO ticket_events VALUES(?,?,?,?,?,?)",
        Store.id(),
        ticket,
        actor,
        type,
        content,
        Store.now());
  }
}
