package cn.liuyang.desk.config;

import cn.liuyang.desk.service.IndexJobProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "desk.jobs.rabbit-enabled", havingValue = "true")
public class IndexQueueConfiguration {
  public static final String QUEUE = "desk.knowledge.index";
  private final IndexJobProcessor processor;

  public IndexQueueConfiguration(IndexJobProcessor processor) {
    this.processor = processor;
  }

  @Bean
  Queue indexQueue() {
    return QueueBuilder.durable(QUEUE)
        .deadLetterExchange("")
        .deadLetterRoutingKey(QUEUE + ".dead")
        .build();
  }

  @Bean
  Queue indexDeadLetterQueue() {
    return QueueBuilder.durable(QUEUE + ".dead").build();
  }

  @RabbitListener(queues = QUEUE)
  public void consume(String jobId) {
    if (jobId == null || !jobId.matches("[a-f0-9-]{36}")) {
      throw new org.springframework.amqp.AmqpRejectAndDontRequeueException(
          "Invalid index job identifier");
    }
    processor.process(jobId);
  }
}
