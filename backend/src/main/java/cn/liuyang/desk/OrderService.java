package cn.liuyang.desk;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final Store s;

    static final String SELECT = "SELECT o.*,p.name product_name,p.category "
            + "FROM shop_orders o JOIN products p ON o.product_id=p.id ";

    public OrderService(Store s) {
        this.s = s;
    }

    public List<Map<String, Object>> list(String owner) {
        return s.db.queryForList(
                SELECT + "WHERE o.owner_id=? ORDER BY o.created_at DESC,o.id",
                owner);
    }

    public Map<String, Object> owned(String owner, String id) {
        return s.one(
                SELECT + "WHERE o.owner_id=? AND (o.id=? OR o.order_no=?)",
                owner, id, id);
    }

    public void eligible(Map<String, Object> order, String kind) {
        if (!"QUESTION".equals(kind) && !"DELIVERED".equals(order.get("status"))) {
            throw new ApiException(409, "该订单尚未签收，可先提交咨询工单");
        }
    }
}
