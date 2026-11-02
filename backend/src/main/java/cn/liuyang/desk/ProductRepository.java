package cn.liuyang.desk;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** JDBC is retained here to compare it with OrderMapper/MyBatis. */
@Repository
public class ProductRepository {
    private final JdbcTemplate db;
    private static final RowMapper<ProductResponse> MAPPER = (row, index) -> new ProductResponse(
            row.getString("id"), row.getString("name"), row.getString("category"),
            row.getLong("price_cents"), row.getString("description"), row.getBoolean("active"));

    public ProductRepository(JdbcTemplate db) { this.db = db; }

    public List<ProductResponse> list(boolean includeInactive) {
        return db.query("SELECT * FROM products " + (includeInactive ? "" : "WHERE active=true ")
                + "ORDER BY id", MAPPER);
    }

    public ProductResponse get(String id) {
        return db.query("SELECT * FROM products WHERE id=?", MAPPER, id).stream()
                .findFirst().orElseThrow(ApiException::missing);
    }

    public void insert(ProductResponse product) {
        db.update("INSERT INTO products(id,name,category,price_cents,description,active) VALUES(?,?,?,?,?,?)",
                product.id(), product.name(), product.category(), product.priceCents(),
                product.description(), product.active());
    }

    public void update(ProductResponse product) {
        if (db.update("UPDATE products SET name=?,category=?,price_cents=?,description=?,active=? WHERE id=?",
                product.name(), product.category(), product.priceCents(), product.description(),
                product.active(), product.id()) != 1) throw ApiException.missing();
    }

    public long revision() {
        return db.queryForObject("SELECT revision FROM catalog_revision WHERE id=1", Long.class);
    }

    public void advanceRevision() {
        db.update("UPDATE catalog_revision SET revision=revision+1 WHERE id=1");
    }
}
