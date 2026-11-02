package cn.liuyang.desk;

import java.util.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {
    private final Store s; private final OrderService orders;
    public static final Set<String> KINDS=Set.of("REFUND","EXCHANGE","REPAIR","QUESTION");
    private static final Map<String,Set<String>> TRANSITIONS=Map.of("OPEN",Set.of("IN_PROGRESS"),"IN_PROGRESS",Set.of("WAITING_CUSTOMER","RESOLVED"),"WAITING_CUSTOMER",Set.of("IN_PROGRESS","RESOLVED"),"RESOLVED",Set.of("IN_PROGRESS","CLOSED"),"CLOSED",Set.of());
    private static final String SELECT="SELECT t.*,u.display_name customer_name,p.name product_name,o.order_no,a.display_name assignee_name FROM tickets t JOIN app_users u ON t.owner_id=u.id LEFT JOIN shop_orders o ON t.order_id=o.id LEFT JOIN products p ON o.product_id=p.id LEFT JOIN app_users a ON t.assignee_id=a.id ";
    public TicketService(Store s,OrderService orders){this.s=s;this.orders=orders;}
    public List<Map<String,Object>> list(String actor,boolean staff){return staff?s.db.queryForList(SELECT+"ORDER BY t.updated_at DESC LIMIT 200"):s.db.queryForList(SELECT+"WHERE t.owner_id=? ORDER BY t.updated_at DESC LIMIT 200",actor);}
    public Map<String,Object> get(String id,String actor,boolean staff){
        var ticket=staff?s.one(SELECT+"WHERE t.id=?",id):s.one(SELECT+"WHERE t.id=? AND t.owner_id=?",id,actor);
        ticket.put("events",s.db.queryForList("SELECT e.*,u.display_name actor_name,u.role actor_role FROM ticket_events e JOIN app_users u ON e.actor_id=u.id WHERE e.ticket_id=? ORDER BY e.created_at,e.id",id));return ticket;
    }
    @Transactional public Map<String,Object> create(String owner,String orderId,String kind,String subject,String description,String key){
        if(!KINDS.contains(kind))throw new ApiException(400,"不支持的工单类型");
        if(key==null||!key.matches("[A-Za-z0-9-]{8,70}"))throw new ApiException(400,"缺少有效的重复提交标识");
        s.one("SELECT id FROM app_users WHERE id=? FOR UPDATE",owner);
        String requestKey=owner+":"+key,hash=Store.hash(s.encode(List.of(orderId==null?"":orderId,kind,subject,description)));
        var existing=s.db.queryForList("SELECT id,request_hash FROM tickets WHERE request_key=?",requestKey);
        if(!existing.isEmpty()){if(!hash.equals(existing.getFirst().get("request_hash")))throw new ApiException(409,"同一提交标识不能用于不同内容");return get((String)existing.getFirst().get("id"),owner,false);}
        String resolved=null;
        if(orderId!=null&&!orderId.isBlank()){var order=orders.owned(owner,orderId);orders.eligible(order,kind);resolved=(String)order.get("id");}
        else if(!"QUESTION".equals(kind))throw new ApiException(400,"售后申请必须关联本人订单");
        String id=Store.id(),now=Store.now(),number="TK-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();
        s.db.update("INSERT INTO tickets(id,ticket_no,owner_id,order_id,kind,subject,description,status,request_key,request_hash,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id,number,owner,resolved,kind,subject,description,"OPEN",requestKey,hash,now,now);
        event(id,owner,"CREATED",description);s.audit(owner,"TICKET_CREATE",id,"创建"+kind+"工单");return get(id,owner,false);
    }
    @Transactional public Map<String,Object> update(String id,String actor,boolean staff,int version,String status,String message){
        var t=s.one("SELECT * FROM tickets WHERE id=? FOR UPDATE",id);
        if(!staff&&!actor.equals(t.get("owner_id")))throw ApiException.missing();
        if(((Number)t.get("version")).intValue()!=version)throw new ApiException(409,"工单已被更新，请刷新后重试");
        String old=(String)t.get("status"),next=status==null||status.isBlank()?old:status;
        if("CLOSED".equals(old))throw new ApiException(409,"工单已关闭，无法继续修改");
        if(!staff&&!next.equals(old)&&!("RESOLVED".equals(old)&&"CLOSED".equals(next)))throw new ApiException(403,"客户只能确认关闭已解决的工单");
        if(!next.equals(old)&&!TRANSITIONS.get(old).contains(next))throw new ApiException(409,"不允许的工单状态变化");
        if((message==null||message.isBlank())&&next.equals(old))throw new ApiException(400,"请输入回复或选择新的状态");
        if(("RESOLVED".equals(next)||"WAITING_CUSTOMER".equals(next))&&!next.equals(old)&&(message==null||message.isBlank()))throw new ApiException(400,"请说明处理结果或需要补充的材料");
        if(!staff&&"WAITING_CUSTOMER".equals(old)&&message!=null&&!message.isBlank())next="IN_PROGRESS";
        String assignee=staff&&t.get("assignee_id")==null?actor:(String)t.get("assignee_id");
        s.db.update("UPDATE tickets SET status=?,assignee_id=?,version=version+1,updated_at=? WHERE id=?",next,assignee,Store.now(),id);
        if(message!=null&&!message.isBlank())event(id,actor,"REPLY",message.trim());
        if(!next.equals(old))event(id,actor,"STATUS",old+" → "+next);
        s.audit(actor,"TICKET_UPDATE",id,old+" → "+next);return get(id,actor,staff);
    }
    @Transactional public Map<String,Object> propose(String owner,String orderId,String kind,String subject,String description){
        if(!KINDS.contains(kind))kind="QUESTION";
        String resolved=null;if(orderId!=null&&!orderId.isBlank()){var order=orders.owned(owner,orderId);orders.eligible(order,kind);resolved=(String)order.get("id");}
        else if(!"QUESTION".equals(kind))throw new ApiException(400,"请提供本人订单号以生成售后草稿");
        String id=Store.id();s.db.update("INSERT INTO proposals VALUES(?,?,?,?,?,?,?,?,?)",id,owner,resolved,kind,subject,description,Instant.now().plus(15,ChronoUnit.MINUTES).toString(),null,Store.now());
        return s.one("SELECT * FROM proposals WHERE id=?",id);
    }
    @Transactional public Map<String,Object> confirm(String id,String owner){
        // Same owner lock order as direct creation; then lock this proposal.
        s.one("SELECT id FROM app_users WHERE id=? FOR UPDATE",owner);
        var p=s.one("SELECT * FROM proposals WHERE id=? AND owner_id=? FOR UPDATE",id,owner);
        if(p.get("ticket_id")!=null)return get((String)p.get("ticket_id"),owner,false);
        if(Instant.parse((String)p.get("expires_at")).isBefore(Instant.now()))throw new ApiException(410,"草稿已过期，请重新生成");
        var t=create(owner,(String)p.get("order_id"),(String)p.get("kind"),(String)p.get("subject"),(String)p.get("description"),id);
        s.db.update("UPDATE proposals SET ticket_id=? WHERE id=?",t.get("id"),id);return t;
    }
    private void event(String ticket,String actor,String type,String content){s.db.update("INSERT INTO ticket_events VALUES(?,?,?,?,?,?)",Store.id(),ticket,actor,type,content,Store.now());}
}
