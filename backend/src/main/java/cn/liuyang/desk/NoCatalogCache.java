package cn.liuyang.desk;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "desk.cache.enabled", havingValue = "false", matchIfMissing = true)
public class NoCatalogCache implements CatalogCache {
    @Override public Optional<List<ProductResponse>> get(long revision) { return Optional.empty(); }
    @Override public void put(long revision, List<ProductResponse> products) { }
}
