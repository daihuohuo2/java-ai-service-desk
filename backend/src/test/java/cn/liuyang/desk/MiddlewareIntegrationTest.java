package cn.liuyang.desk;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="MIDDLEWARE_TESTS", matches="true")
@SpringBootTest(properties={
    "spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:middleware_tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${TEST_DATABASE_USER:sa}",
    "spring.datasource.password=${TEST_DATABASE_PASSWORD:}",
    "desk.demo=true", "desk.cache.enabled=true", "desk.cache.namespace=desk-fullstack-it",
    "desk.jobs.rabbit-enabled=true", "desk.jobs.worker-enabled=false",
    "spring.data.redis.port=${TEST_REDIS_PORT:26379}",
    "spring.rabbitmq.port=${TEST_RABBITMQ_PORT:25672}",
    "spring.rabbitmq.username=desk_test", "spring.rabbitmq.password=test-only-password"
})
class MiddlewareIntegrationTest {
    @Autowired ProductService products;
    @Autowired ProductRepository repository;
    @Autowired RedisCatalogCache cache;
    @Autowired StringRedisTemplate redis;
    @Autowired RabbitTemplate rabbit;
    @Autowired IndexJobService jobs;
    @Autowired IndexJobProcessor processor;
    @MockitoBean AiGateway ai;

    @BeforeEach void model() {
        when(ai.embed(anyString())).thenReturn(new float[]{1,0,0});
        when(ai.embeddingName()).thenReturn("integration-model");
    }

    @Test void realRedisStoresCatalogAndNewRevisionIgnoresOldEntry() {
        var product=products.save(null,new ProductInput("缓存测试商品","学习",100,"初始",true),"admin");
        try {
            long before=repository.revision();
            products.active();
            assertNotNull(redis.opsForValue().get(cache.key(before)));
            assertTrue(redis.getExpire(cache.key(before))>0);
            products.save(product.id(),new ProductInput("缓存测试商品","学习",200,"更新",true),"admin");
            assertNotEquals(before,repository.revision());
            assertEquals(200,products.active().stream().filter(p->p.id().equals(product.id())).findFirst().orElseThrow().priceCents());
        } finally {
            products.save(product.id(),new ProductInput("缓存测试商品","学习",200,"测试结束",false),"admin");
        }
    }

    @Test void realRabbitDeliversDuplicateMessagesOnlyOnce() throws Exception {
        var job=jobs.submit("doc-lamp","admin",UUID.randomUUID().toString());
        rabbit.convertAndSend("",IndexQueueConfiguration.QUEUE,job.id());
        rabbit.convertAndSend("",IndexQueueConfiguration.QUEUE,job.id());
        long deadline=System.nanoTime()+Duration.ofSeconds(20).toNanos();
        while(!"SUCCEEDED".equals(jobs.get(job.id()).state()) && System.nanoTime()<deadline) Thread.sleep(100);
        assertEquals("SUCCEEDED",jobs.get(job.id()).state());
        assertEquals(1,jobs.get(job.id()).attempts());
        verify(ai,times(3)).embed(anyString());
    }

    @Test void brokerFailureLeavesDurableJobAvailableForRedelivery() {
        var job=jobs.submit("doc-lamp","admin",UUID.randomUUID().toString());
        var unavailable=mock(RabbitTemplate.class);
        doThrow(new org.springframework.amqp.AmqpConnectException(new java.net.ConnectException("offline")))
                .when(unavailable).convertAndSend(eq(""),eq(IndexQueueConfiguration.QUEUE),any(Object.class));
        new IndexJobDispatcher(jobs,processor,unavailable,true).dispatch();
        assertEquals("PENDING",jobs.get(job.id()).state());
        assertTrue(jobs.ready(20).contains(job.id()));
        processor.process(job.id());
        assertEquals("SUCCEEDED",jobs.get(job.id()).state());
    }
}
