package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.PriceCart;
import com.example.cart.domain.PriceCartResult;
import com.example.cart.domain.PricedCart;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.Path;
import org.jooq.DSLContext;
import org.jooq.Record;

/**
 * {@code priceCart} INJECTED 振る舞いの jOOQ 実装。カート全明細を読み、各明細ごとに商品行を引いて
 * 販売中・存在を確かめ、価格を載せた {@link com.example.cart.domain.OrderLine} の Map を作る。
 * 全明細分を集めて {@link PricedCart} にデコードして返す。商品なし・販売終了は抽象基底のファクトリで短絡する。
 *
 * <p>この「明細ごとに INJECTED な参照を呼ぶ」ループが Java 側に残るのは、Souther に traverse が無く、
 * map/fold の中から別の INJECTED 振る舞いを呼べないため（friction）。ここは gateway の内側で閉じる。
 */
public final class JooqPriceCart extends PriceCart {

    private final DSLContext dsl;

    public JooqPriceCart(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public PriceCartResult apply(com.example.cart.domain.UserId userId) {
        UUID cartId = cartIdOf(userId);
        List<Map<String, Object>> lineMaps = new ArrayList<>();
        if (cartId != null) {
            org.jooq.Result<Record> rows = dsl.fetch("""
                    SELECT product_id, quantity
                    FROM cart_item
                    WHERE cart_id = ?
                    ORDER BY product_id
                    """, cartId);
            for (Record row : rows) {
                UUID productId = row.get(0, UUID.class);
                long quantity = row.get(1, Integer.class);

                Record product = dsl.fetchOne(
                        "SELECT on_sale, price FROM product WHERE product_id = ?", productId);
                if (product == null) {
                    return ProductNotFound();
                }
                if (!product.get(0, Boolean.class)) {
                    return SaleEnded();
                }
                long price = product.get(1, Long.class);

                Map<String, Object> line = new LinkedHashMap<>();
                line.put("productId", productId.toString());
                line.put("quantity", quantity);
                line.put("unitPrice", price);
                lineMaps.add(line);
            }
        }
        return PricedCart.decoder().decode(Map.of("lines", lineMaps), Path.ROOT).getOrThrow();
    }

    private UUID cartIdOf(com.example.cart.domain.UserId userId) {
        return dsl.select(field("cart_id", UUID.class))
                .from(table("cart"))
                .where(field("user_id", UUID.class).eq(UUID.fromString(userId.value())))
                .fetchOne(field("cart_id", UUID.class));
    }
}
