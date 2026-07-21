package com.example.cart.infrastructure;

import com.example.cart.domain.Product;
import com.example.cart.domain.ProductId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.Path;
import net.unit8.raoh.Result;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * 商品参照の gateway。行を取り、列値を Souther の {@link Product} の（フィールド名キーの）Map に組み直して
 * {@code Product.decoder()} でデコードして返す。ここで ProductId / Money の不変条件が再検査される
 * ——これが raoh-souther の境界（DB→ドメイン）の継ぎ目。
 */
@Repository
public class ProductRepository {

    private final DSLContext dsl;

    public ProductRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Result<Product> load(ProductId productId) {
        Record row = dsl.fetchOne(
                "SELECT product_id, on_sale, price FROM product WHERE product_id = ?",
                UUID.fromString(productId.value()));
        if (row == null) {
            return Result.fail(Path.ROOT, "product_not_found", "商品が見つかりません");
        }
        UUID id = row.get(0, UUID.class);
        boolean onSale = row.get(1, Boolean.class);
        long price = row.get(2, Long.class);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id.toString());
        m.put("onSale", onSale);
        m.put("price", price);
        return Product.decoder().decode(m, Path.ROOT);
    }
}
