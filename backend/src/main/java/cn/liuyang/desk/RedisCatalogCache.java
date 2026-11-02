package cn.liuyang.desk;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "desk.cache.enabled", havingValue = "true")
public class RedisCatalogCache implements CatalogCache {
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final MeterRegistry metrics;
    private final String namespace;

    public RedisCatalogCache(StringRedisTemplate redis, ObjectMapper json, MeterRegistry metrics,
            @Value("${desk.cache.namespace:desk}") String namespace) {
        this.redis = redis;
        this.json = json;
        this.metrics = metrics;
        this.namespace = namespace;
    }

    String key(long revision) { return namespace + ":catalog:v" + revision; }

    @Override
    public Optional<List<ProductResponse>> get(long revision) {
        try {
            String value = redis.opsForValue().get(key(revision));
            if (value != null) {
                var products = Arrays.asList(json.readValue(value, ProductResponse[].class));
                metrics.counter("desk.catalog.cache", "result", "hit").increment();
                return Optional.of(products);
            }
            metrics.counter("desk.catalog.cache", "result", "miss").increment();
        } catch (RuntimeException e) {
            metrics.counter("desk.catalog.cache", "result", "error").increment();
        }
        return Optional.empty();
    }

    @Override
    public void put(long revision, List<ProductResponse> products) {
        try {
            redis.opsForValue().set(key(revision), json.writeValueAsString(products), Duration.ofSeconds(30));
        } catch (RuntimeException e) {
            metrics.counter("desk.catalog.cache", "result", "error").increment();
        }
    }
}
