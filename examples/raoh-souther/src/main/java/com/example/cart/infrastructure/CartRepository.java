package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.Cart;
import com.example.cart.domain.CartId;
import com.example.cart.domain.CartItem;
import com.example.cart.domain.UserId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.Path;
import net.unit8.raoh.Result;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * 更新用リポジトリ。{@link #loadForUpdate} は cart と cart_item を LEFT JOIN した集計クエリ1本で
 * {@code (cart_id, total_quantity)} を1行取り、Souther の {@link Cart} にデコードして返す。全アイテムは
 * ロードしない（性能）まま、上限判断に必要な合計数量を型に載せる（完全性）。
 */
@Repository
public class CartRepository {

    private final DSLContext dsl;

    public CartRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Result<Cart> loadForUpdate(UserId userId) {
        UUID uid = UUID.fromString(userId.value());
        ensureCartExists(uid);
        Record row = dsl.fetchOne("""
                SELECT c.cart_id, COALESCE(SUM(ci.quantity), 0)
                FROM cart c
                LEFT JOIN cart_item ci ON ci.cart_id = c.cart_id
                WHERE c.user_id = ?
                GROUP BY c.cart_id
                """, uid);
        UUID cartId = row.get(0, UUID.class);
        long total = row.get(1, Long.class);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", cartId.toString());
        m.put("currentQuantity", total);
        return Cart.decoder().decode(m, Path.ROOT);
    }

    public void addItem(CartId cartId, CartItem item) {
        UUID cid = UUID.fromString(cartId.value());
        UUID productId = UUID.fromString(item.productId().value());
        int quantity = (int) item.quantity().value();

        int updated = dsl.update(table("cart_item"))
                .set(field("quantity", Integer.class), field("quantity", Integer.class).plus(quantity))
                .where(field("cart_id", UUID.class).eq(cid))
                .and(field("product_id", UUID.class).eq(productId))
                .execute();
        if (updated == 0) {
            dsl.insertInto(table("cart_item"))
                    .columns(field("cart_item_id"), field("cart_id"), field("product_id"), field("quantity"))
                    .values(UUID.randomUUID(), cid, productId, quantity)
                    .execute();
        }
    }

    private void ensureCartExists(UUID userId) {
        UUID existing = dsl.select(field("cart_id", UUID.class))
                .from(table("cart"))
                .where(field("user_id", UUID.class).eq(userId))
                .fetchOne(field("cart_id", UUID.class));
        if (existing == null) {
            dsl.insertInto(table("cart"))
                    .columns(field("cart_id"), field("user_id"))
                    .values(UUID.randomUUID(), userId)
                    .execute();
        }
    }
}
