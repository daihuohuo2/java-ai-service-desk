package cn.liuyang.desk.service;

import cn.liuyang.desk.cache.CatalogCache;
import cn.liuyang.desk.dto.ProductInput;
import cn.liuyang.desk.dto.ProductResponse;
import cn.liuyang.desk.repository.ProductRepository;
import cn.liuyang.desk.repository.Store;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
  private final ProductRepository products;
  private final CatalogCache cache;
  private final Store store;

  public ProductService(ProductRepository products, CatalogCache cache, Store store) {
    this.products = products;
    this.cache = cache;
    this.store = store;
  }

  public List<ProductResponse> active() {
    if (org.springframework.transaction.support.TransactionSynchronizationManager
        .isActualTransactionActive()) {
      return products.list(false); // Never publish uncommitted rows into a shared cache.
    }
    // SQL owns the generation. Redis failures cannot resurrect old cache keys.
    long revision = products.revision();
    return cache
        .get(revision)
        .orElseGet(
            () -> {
              var rows = products.list(false);
              cache.put(revision, rows);
              return rows;
            });
  }

  public List<ProductResponse> all() {
    return products.list(true);
  }

  @Transactional
  public ProductResponse save(String id, ProductInput input, String actor) {
    boolean create = id == null;
    var product =
        new ProductResponse(
            create ? Store.id() : id,
            input.name().trim(),
            input.category().trim(),
            input.priceCents(),
            input.description().trim(),
            input.active());
    if (create) products.insert(product);
    else products.update(product);
    products.advanceRevision();
    store.audit(actor, create ? "PRODUCT_CREATE" : "PRODUCT_UPDATE", product.id(), "保存商品");
    return product;
  }
}
