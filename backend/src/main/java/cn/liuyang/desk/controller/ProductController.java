package cn.liuyang.desk.controller;

import cn.liuyang.desk.dto.ProductInput;
import cn.liuyang.desk.dto.ProductResponse;
import cn.liuyang.desk.security.Actor;
import cn.liuyang.desk.service.ProductService;
import org.springframework.validation.annotation.Validated;
import cn.liuyang.desk.common.validation.ValidationGroups;
import cn.liuyang.desk.common.ApiException;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProductController {
  private final ProductService products;
  private final Actor actor;

  public ProductController(ProductService products, Actor actor) {
    this.products = products;
    this.actor = actor;
  }

  @GetMapping("/products")
  public List<ProductResponse> active() {
    return products.active();
  }

  @GetMapping("/admin/products")
  public List<ProductResponse> all() {
    return products.all();
  }

  @PostMapping("/admin/products")
  public ProductResponse create(@Validated(ValidationGroups.Create.class) @RequestBody ProductInput input) {
    return products.save(null, input, actor.id());
  }

  @PutMapping("/admin/products/{id}")
  public ProductResponse update(@PathVariable String id, @Validated(ValidationGroups.Update.class) @RequestBody ProductInput input) {
    if (!id.equals(input.id())) {
      throw new ApiException(400, "路径编号和请求中的商品编号必须一致");
    }
    return products.save(id, input, actor.id());
  }
}
