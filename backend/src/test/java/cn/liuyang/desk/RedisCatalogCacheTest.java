package cn.liuyang.desk;

import java.time.Duration;
import java.util.List;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisCatalogCacheTest {
    @SuppressWarnings("unchecked")
    @Test void cacheRoundTripHasTypedProductsAndDifferentGenerations() {
        var redis=mock(StringRedisTemplate.class);
        ValueOperations<String,String> values=mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var json=JsonMapper.builder().build();
        var product=new ProductResponse("p","灯","照明",199,"说明",true);
        when(values.get("test:catalog:v1")).thenReturn(json.writeValueAsString(List.of(product)));
        var cache=new RedisCatalogCache(redis,json,new SimpleMeterRegistry(),"test");
        assertEquals(product,cache.get(1).orElseThrow().getFirst());
        assertTrue(cache.get(2).isEmpty());
        cache.put(2,List.of(product));
        verify(values).set(eq("test:catalog:v2"),anyString(),eq(Duration.ofSeconds(30)));
    }

    @Test void redisOutageIsACacheMissAndWritesDoNotFailBusiness() {
        var redis=mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("offline"));
        var cache=new RedisCatalogCache(redis,JsonMapper.builder().build(),new SimpleMeterRegistry(),"test");
        assertTrue(cache.get(1).isEmpty());
        assertDoesNotThrow(()->cache.put(1,List.of()));
    }

    @SuppressWarnings("unchecked")
    @Test void malformedEntryFallsBackToDatabase() {
        var redis=mock(StringRedisTemplate.class);
        ValueOperations<String,String> values=mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn("not-json");
        var cache=new RedisCatalogCache(redis,JsonMapper.builder().build(),new SimpleMeterRegistry(),"test");
        assertTrue(cache.get(1).isEmpty());
    }

    @Test void csvEscapesLeadingWhitespaceFormulasAndNewlines() {
        assertEquals("\"'  =1+1\"",CsvExport.cell("  =1+1"));
        assertEquals("\"'\tvalue\"",CsvExport.cell("\tvalue"));
        assertEquals("\"a\n\"\"b\"\"\"",CsvExport.cell("a\n\"b\""));
    }
}
