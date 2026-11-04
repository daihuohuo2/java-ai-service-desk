import java.util.*;

/**
 * Run: java learning/FullstackBasics.java Read in order: Order -> OrderRepository -> MemoryOrders
 * -> OrderService -> main.
 */
public class FullstackBasics {
  record Order(String id, String owner, String status, long priceCents) {
    Order {
      if (priceCents < 0) throw new IllegalArgumentException("Price cannot be negative");
    }
  }

  interface OrderRepository {
    List<Order> findByOwner(String owner);
  }

  static class MemoryOrders implements OrderRepository {
    private final List<Order> rows;

    MemoryOrders(List<Order> rows) {
      this.rows = List.copyOf(rows);
    }

    @Override
    public List<Order> findByOwner(String owner) {
      return rows.stream().filter(order -> order.owner().equals(owner)).toList();
    }
  }

  static class OrderService {
    private final OrderRepository repository;

    OrderService(OrderRepository repository) {
      this.repository = repository;
    }

    List<Order> page(String owner, int page, int size) {
      if (page < 1 || size < 1) throw new IllegalArgumentException("Invalid page");
      return repository.findByOwner(owner).stream()
          .sorted(Comparator.comparing(Order::id))
          .skip((long) (page - 1) * size)
          .limit(size)
          .toList();
    }
  }

  static <T> List<T> distinctKeepingOrder(List<T> items) {
    // LinkedHashSet uses equals/hashCode and keeps insertion order.
    return new ArrayList<>(new LinkedHashSet<>(items));
  }

  public static void main(String[] args) {
    var first = new Order("1", "customer", "DELIVERED", 19900);
    var repository =
        new MemoryOrders(
            List.of(
                first,
                new Order("2", "customer", "SHIPPED", 12900),
                new Order("3", "other", "DELIVERED", 29900)));
    var service = new OrderService(repository);
    check(service.page("customer", 1, 1).equals(List.of(first)), "page and ownership");
    check(service.page("customer", 2, 1).getFirst().id().equals("2"), "second page");
    check(distinctKeepingOrder(List.of(first, first)).size() == 1, "record equality");
    check(distinctKeepingOrder(List.of("a", "b", "a")).equals(List.of("a", "b")), "generic method");
    try {
      service.page("customer", 0, 10);
      throw new AssertionError("Expected validation");
    } catch (IllegalArgumentException expected) {
      System.out.println("PASS: invalid page rejected");
    }
    System.out.println("FullstackBasics: all five checks passed.");
  }

  static void check(boolean valid, String message) {
    if (!valid) throw new AssertionError(message);
    System.out.println("PASS: " + message);
  }
}
