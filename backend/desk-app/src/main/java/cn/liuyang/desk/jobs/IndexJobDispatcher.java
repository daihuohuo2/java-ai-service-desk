package cn.liuyang.desk.jobs;

import cn.liuyang.desk.config.IndexQueueConfiguration;
import cn.liuyang.desk.service.IndexJobProcessor;
import cn.liuyang.desk.service.IndexJobService;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "desk.jobs.worker-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class IndexJobDispatcher {
  private final IndexJobService jobs;
  private final IndexJobProcessor processor;
  private final RabbitTemplate rabbit;
  private final boolean useRabbit;

  public IndexJobDispatcher(
      IndexJobService jobs,
      IndexJobProcessor processor,
      RabbitTemplate rabbit,
      @Value("${desk.jobs.rabbit-enabled:false}") boolean useRabbit) {
    this.jobs = jobs;
    this.processor = processor;
    this.rabbit = rabbit;
    this.useRabbit = useRabbit;
  }

  @Scheduled(fixedDelayString = "${desk.jobs.poll-ms:5000}", initialDelay = 5000)
  public void dispatch() {
    // Repeat delivery is intentional: a crash before acknowledgement cannot lose the job.
    for (String id : jobs.ready(useRabbit ? 20 : 1)) {
      try {
        if (useRabbit) {
          rabbit.convertAndSend("", IndexQueueConfiguration.QUEUE, id);
        } else processor.process(id);
      } catch (RuntimeException failure) {
        LoggerFactory.getLogger(getClass())
            .warn("Index delivery unavailable; durable job retained");
        break;
      }
    }
  }
}
