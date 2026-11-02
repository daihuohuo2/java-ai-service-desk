import java.util.ArrayList;
import java.util.List;

// 纯 Java 教学例子：不连接数据库，不启动 Spring，不影响真实项目数据。
public class OrderLesson {
    record Order(String number, String owner, String product, int priceCents) {}

    public static void main(String[] args) {
        List<Order> allOrders = List.of(
            new Order("ZS20261030001", "customer", "阅读灯", 19900),
            new Order("ZS20261030002", "customer", "保温杯", 12900),
            new Order("ZS20261030003", "customer2", "耳机", 29900)
        );

        String currentUser = "customer";
        List<Order> myOrders = findMyOrders(allOrders, currentUser);
        System.out.println("当前用户：" + currentUser);
        System.out.println("能查看的订单数：" + myOrders.size());
        for (Order order : myOrders) {
            System.out.println(order.number() + " | " + order.product() + " | " + order.priceCents() + " 分");
        }

        String requestedNumber = "ZS20261030003";
        Order found = findOwnedOrder(allOrders, currentUser, requestedNumber);
        System.out.println("尝试查询：" + requestedNumber);
        System.out.println(found == null ? "未找到本人订单" : "查询成功：" + found.product());
    }

    static List<Order> findMyOrders(List<Order> allOrders, String owner) {
        List<Order> result = new ArrayList<>();
        for (Order order : allOrders) {
            if (owner.equals(order.owner())) {
                result.add(order);
            }
        }
        return result;
    }

    static Order findOwnedOrder(List<Order> allOrders, String owner, String number) {
        for (Order order : allOrders) {
            if (owner.equals(order.owner()) && number.equals(order.number())) {
                return order;
            }
        }
        return null;
    }
}
