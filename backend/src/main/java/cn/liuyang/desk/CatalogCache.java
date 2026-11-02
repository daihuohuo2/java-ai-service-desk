package cn.liuyang.desk;

import java.util.List;
import java.util.Optional;

/** Business code works with either Redis or the no-cache implementation. */
public interface CatalogCache {
    Optional<List<ProductResponse>> get(long revision);
    void put(long revision, List<ProductResponse> products);
}
