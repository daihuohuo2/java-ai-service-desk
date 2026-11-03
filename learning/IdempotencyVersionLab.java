import java.util.HashMap;
import java.util.Map;

/** Run with JDK 21: java learning/IdempotencyVersionLab.java */
public class IdempotencyVersionLab {
  record Receipt(String id, String body) {}

  static final class IdempotentDesk {
    private final Map<String, Receipt> requests = new HashMap<>();
    private int sequence;

    synchronized Receipt create(String owner, String key, String body) {
      String requestKey = owner + ":" + key;
      Receipt existing = requests.get(requestKey);
      if (existing != null) {
        if (!existing.body().equals(body)) {
          throw new IllegalStateException("Same key, different content");
        }
        return existing;
      }
      Receipt created = new Receipt("T-" + ++sequence, body);
      requests.put(requestKey, created);
      return created;
    }
  }

  static final class VersionedTicket {
    private int version;
    private String status = "OPEN";

    synchronized void update(int expectedVersion, String next) {
      if (version != expectedVersion) {
        throw new IllegalStateException("Stale version");
      }
      status = next;
      version++;
    }
  }

  public static void main(String[] args) {
    // Stage 1: treating every click as a new command produces two records.
    int naiveCount = 0;
    naiveCount++;
    naiveCount++;
    require(naiveCount == 2, "Naive duplicate submission demonstration");
    System.out.println("1. Without idempotency: two clicks create two records.");

    // Stage 2: recognize the same operation; scope the key to the owner.
    var desk = new IdempotentDesk();
    var first = desk.create("customer", "click-001", "Question about warranty");
    var retry = desk.create("customer", "click-001", "Question about warranty");
    require(first.id().equals(retry.id()), "A retry must return the original receipt");
    var other = desk.create("customer2", "click-001", "Question about warranty");
    require(!first.id().equals(other.id()), "Different owners must not share receipts");
    System.out.println("2. Same owner + same key + same body: one receipt.");

    // Stage 3: an idempotency key cannot authorize a different operation.
    boolean rejected = false;
    try {
      desk.create("customer", "click-001", "A different request");
    } catch (IllegalStateException expected) {
      rejected = true;
    }
    require(rejected, "A changed body with the same key must fail");
    System.out.println("3. Same key + different body: conflict.");

    // Stage 4: two agents both read version 0; the second write must not overwrite the first.
    var ticket = new VersionedTicket();
    int versionReadByA = ticket.version;
    int versionReadByB = ticket.version;
    ticket.update(versionReadByA, "IN_PROGRESS");
    rejected = false;
    try {
      ticket.update(versionReadByB, "RESOLVED");
    } catch (IllegalStateException expected) {
      rejected = true;
    }
    require(
        rejected && ticket.version == 1 && ticket.status.equals("IN_PROGRESS"),
        "A stale write must preserve the first accepted update");
    System.out.println("4. Stale version: conflict; first update preserved.");
    System.out.println(
        "PASS. In-memory, single-process lesson. Real services also need transactions and DB"
            + " constraints.");
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
