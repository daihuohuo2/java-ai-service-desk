package cn.liuyang.desk.repository;

import cn.liuyang.desk.common.ApiException;
import cn.liuyang.desk.converter.DeskConverter;
import cn.liuyang.desk.dto.ProductResponse;
import cn.liuyang.desk.entity.Product;
import cn.liuyang.desk.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Product CRUD uses MyBatis-Plus; the shared cache revision uses explicit atomic SQL. */
@Repository
@RequiredArgsConstructor
public class ProductRepository {
  private final ProductMapper mapper;
  private final DeskConverter converter;
  private final JdbcTemplate db;

  public List<ProductResponse> list(boolean includeInactive) {
    var query = new LambdaQueryWrapper<Product>();
    query.eq(!includeInactive, Product::isActive, true).orderByAsc(Product::getId);
    return mapper.selectList(query).stream().map(converter::product).toList();
  }

  public ProductResponse get(String id) {
    Product product = mapper.selectById(id);
    if (product == null) {
      throw ApiException.missing();
    }
    return converter.product(product);
  }

  public void insert(ProductResponse product) {
    mapper.insert(converter.productEntity(product));
  }

  public void update(ProductResponse product) {
    if (mapper.updateById(converter.productEntity(product)) != 1) {
      throw ApiException.missing();
    }
  }

  public long revision() {
    return db.queryForObject("SELECT revision FROM catalog_revision WHERE id=1", Long.class);
  }

  public void advanceRevision() {
    db.update("UPDATE catalog_revision SET revision=revision+1 WHERE id=1");
  }
}
