package com.example.cart.infrastructure;

import com.example.cart.domain.Product;
import com.example.cart.domain.ProductId;
import com.example.cart.domain.ProductRepository;
import net.unit8.raoh.Path;
import net.unit8.raoh.Result;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * 商品参照の gateway。行を取り、{@link JooqProductDecoders#PRODUCT} で {@link Product} にデコードして返す。
 */
@Repository
public class JooqProductRepository implements ProductRepository {

    private final DSLContext dsl;

    public JooqProductRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Result<Product> load(ProductId productId) {
        // 別名を小文字でクォートして固定（H2 の無引用識別子は大文字化されるため）。
        Record row = dsl.fetchOne("""
                SELECT product_id AS "product_id", on_sale AS "on_sale", price AS "price"
                FROM product
                WHERE product_id = ?
                """, productId.value());
        if (row == null) {
            return Result.fail(Path.ROOT, "product_not_found", "商品が見つかりません");
        }
        return JooqProductDecoders.PRODUCT.decode(row);
    }
}
