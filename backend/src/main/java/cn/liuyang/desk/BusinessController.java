package cn.liuyang.desk;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BusinessController {
    private final Store s; private final Actor actor; private final OrderService orders; private final TicketService tickets;
    public BusinessController(Store s,Actor actor,OrderService orders,TicketService tickets){this.s=s;this.actor=actor;this.orders=orders;this.tickets=tickets;}
    @GetMapping("/products") Object products(){return s.db.queryForList("SELECT * FROM products WHERE active=true ORDER BY id");}
    @GetMapping("/orders") Object orders(){return orders.list(actor.id());}
    @GetMapping("/orders/{id}") Object order(@PathVariable String id){return orders.owned(actor.id(),id);}
    @GetMapping("/tickets") Object tickets(){return tickets.list(actor.id(),actor.staff());}
    @GetMapping("/tickets/{id}") Object ticket(@PathVariable String id){return tickets.get(id,actor.id(),actor.staff());}
    record NewTicket(@Size(max=40) String orderId,@NotBlank String kind,@NotBlank @Size(max=120) String subject,@NotBlank @Size(max=2000) String description){}
    @PostMapping("/tickets") Object create(@RequestHeader(value="Idempotency-Key",required=false)String key,@Valid @RequestBody NewTicket request){return tickets.create(actor.id(),request.orderId(),request.kind(),request.subject().trim(),request.description().trim(),key);}
    record TicketUpdate(@Min(0) int version,@Size(max=30) String status,@Size(max=2000) String message){}
    @PostMapping("/tickets/{id}/events") Object update(@PathVariable String id,@Valid @RequestBody TicketUpdate request){return tickets.update(id,actor.id(),actor.staff(),request.version(),request.status(),request.message());}
    @PostMapping("/proposals/{id}/confirm") Object confirm(@PathVariable String id){return tickets.confirm(id,actor.id());}
    @GetMapping("/admin/overview") Object overview(){
        return Map.of("tickets",s.db.queryForList("SELECT status,COUNT(*) total FROM tickets GROUP BY status"),"orders",s.db.queryForObject("SELECT COUNT(*) FROM shop_orders",Integer.class),"knowledge",s.db.queryForObject("SELECT COUNT(*) FROM knowledge_documents WHERE active=true",Integer.class),"modelRuns",s.db.queryForObject("SELECT COUNT(*) FROM chat_runs",Integer.class),"recentRuns",s.db.queryForList("SELECT id,state,model_mode,latency_ms,created_at FROM chat_runs ORDER BY created_at DESC LIMIT 8"));
    }
    @GetMapping("/admin/audit") Object audit(){return s.db.queryForList("SELECT a.*,u.display_name actor_name FROM audit_log a LEFT JOIN app_users u ON a.actor_id=u.id ORDER BY created_at DESC LIMIT 100");}
    record ProductInput(@NotBlank @Size(max=120)String name,@NotBlank @Size(max=80)String category,@Min(0) @Max(100000000) long priceCents,@NotBlank @Size(max=2000)String description,boolean active){}
    @GetMapping("/admin/products") Object allProducts(){return s.db.queryForList("SELECT * FROM products ORDER BY id");}
    @PostMapping("/admin/products") Object createProduct(@Valid @RequestBody ProductInput r){String id=Store.id();s.db.update("INSERT INTO products VALUES(?,?,?,?,?,?)",id,r.name(),r.category(),r.priceCents(),r.description(),r.active());s.audit(actor.id(),"PRODUCT_CREATE",id,"新增商品");return s.one("SELECT * FROM products WHERE id=?",id);}
    @PutMapping("/admin/products/{id}") Object updateProduct(@PathVariable String id,@Valid @RequestBody ProductInput r){s.one("SELECT id FROM products WHERE id=?",id);s.db.update("UPDATE products SET name=?,category=?,price_cents=?,description=?,active=? WHERE id=?",r.name(),r.category(),r.priceCents(),r.description(),r.active(),id);s.audit(actor.id(),"PRODUCT_UPDATE",id,"更新商品");return s.one("SELECT * FROM products WHERE id=?",id);}
}
