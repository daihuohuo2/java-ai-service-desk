import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Run: java learning/ConcurrencyLab.java. No Spring, database or external service. */
public class ConcurrencyLab {
  public static void main(String[] args) throws Exception {
    var permits = new Semaphore(2);
    var active = new AtomicInteger();
    var maximum = new AtomicInteger();
    var completed = new AtomicInteger();
    try (var workers = Executors.newFixedThreadPool(4)) {
      var tasks = new ArrayList<Future<?>>();
      for (int i = 0; i < 40; i++) {
        tasks.add(
            workers.submit(
                () -> {
                  boolean acquired = false;
                  try {
                    permits.acquire();
                    acquired = true;
                    int count = active.incrementAndGet();
                    maximum.accumulateAndGet(count, Math::max);
                    try {
                      Thread.sleep(5);
                      completed.incrementAndGet();
                    } finally {
                      active.decrementAndGet();
                    }
                  } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                  } finally {
                    if (acquired) permits.release();
                  }
                }));
      }
      for (var task : tasks) task.get(5, TimeUnit.SECONDS);
      if (completed.get() != 40 || maximum.get() > 2 || permits.availablePermits() != 2)
        throw new AssertionError("Concurrency limit or resource release failed");

      var started = new CountDownLatch(1);
      var blocker = new CountDownLatch(1);
      Future<?> waiting =
          workers.submit(
              () -> {
                started.countDown();
                try {
                  blocker.await();
                } catch (InterruptedException interrupted) {
                  Thread.currentThread().interrupt();
                }
              });
      if (!started.await(5, TimeUnit.SECONDS)) throw new AssertionError("Task did not start");
      try {
        waiting.get(20, TimeUnit.MILLISECONDS);
        throw new AssertionError("Expected timeout");
      } catch (TimeoutException expected) {
        waiting.cancel(true);
      } finally {
        blocker.countDown();
      }
      if (!waiting.isCancelled()) throw new AssertionError("Cancellation was not requested");
    }
    System.out.println(
        "PASS: 40 tasks; max concurrent work <= 2; permits restored; timeout cancellation.");
    System.out.println(
        "Cancellation is cooperative: a remote request may still finish on its server.");
  }
}
