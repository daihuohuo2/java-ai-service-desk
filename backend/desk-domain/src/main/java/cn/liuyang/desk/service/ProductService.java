package cn.liuyang.desk.service;

import cn.liuyang.desk.dto.ProductInput;
import cn.liuyang.desk.dto.ProductResponse;
import java.util.List;

/** Business contract; see service.impl.ProductServiceImpl for execution. */
public interface ProductService {
  List<ProductResponse> active();

  List<ProductResponse> all();

  ProductResponse save(String id, ProductInput input, String actor);
}
