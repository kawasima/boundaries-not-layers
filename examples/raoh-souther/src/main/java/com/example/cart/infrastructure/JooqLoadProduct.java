package com.example.cart.infrastructure;

import com.example.cart.domain.LoadProduct;
import com.example.cart.domain.LoadProductResult;
import com.example.cart.domain.Product;
import com.example.cart.domain.ProductId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.Path;
import org.jooq.DSLContext;
import org.jooq.Record;

/**
 * {@code loadProduct} INJECTED 振る舞いの jOOQ 実装。行を取り、列値を Souther の {@link Product} の
 * （フィールド名キーの）Map に組み直して {@code Product.decoder()} でデコードして返す。ここで
 * ProductId / Money の不変条件が再検査される——これが DB→ドメインの継ぎ目。行が無ければ、抽象基底が
 * 提供する {@link #ProductNotFound()} ファクトリで失敗ケースを返す（{@code new} しない）。
 */
public final class JooqLoadProduct extends LoadProduct {

    private final DSLContext dsl;

    public JooqLoadProduct(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public LoadProductResult apply(ProductId productId) {
        Record row = dsl.fetchOne(
                "SELECT product_id, on_sale, price FROM product WHERE product_id = ?",
                UUID.fromString(productId.value()));
        if (row == null) {
            return ProductNotFound();
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", row.get(0, UUID.class).toString());
        m.put("onSale", row.get(1, Boolean.class));
        m.put("price", row.get(2, Long.class));
        return Product.decoder().decode(m, Path.ROOT).getOrThrow();
    }
}
