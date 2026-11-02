package cn.liuyang.desk;

import java.util.List;
import jakarta.validation.Valid;
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

    @GetMapping("/products") public List<ProductResponse> active() { return products.active(); }
    @GetMapping("/admin/products") public List<ProductResponse> all() { return products.all(); }

    @PostMapping("/admin/products")
    public ProductResponse create(@Valid @RequestBody ProductInput input) {
        return products.save(null, input, actor.id());
    }

    @PutMapping("/admin/products/{id}")
    public ProductResponse update(@PathVariable String id, @Valid @RequestBody ProductInput input) {
        return products.save(id, input, actor.id());
    }
}
