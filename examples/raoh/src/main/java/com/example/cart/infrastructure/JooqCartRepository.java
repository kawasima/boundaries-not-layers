package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.Cart;
import com.example.cart.domain.CartId;
import com.example.cart.domain.CartItem;
import com.example.cart.domain.CartRepository;
import com.example.cart.domain.UserId;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.Result;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/**
 * 性能+完全性版の更新用リポジトリ（raoh 版）。
 *
 * <p>{@link #loadForUpdate} は cart と cart_item を LEFT JOIN した集計クエリ1本で
 * {@code (cart_id, total_quantity)} を1行取り、{@link JooqCartDecoders#CART} で {@link Cart} に
 * デコードして {@link Result} で返す。全アイテムをロードしない。ドメインの不変条件チェックは
 * この Cart 上で純粋に行われ、リポジトリはドメインの内側から呼ばれない。
 */
@Repository
public class JooqCartRepository implements CartRepository {

    private final DSLContext dsl;

    public JooqCartRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Result<Cart> loadForUpdate(UserId userId) {
        ensureCartExists(userId.value());
        // 集計クエリ1本。アイテムが増えても LEFT JOIN + SUM の一行に集約されるので重くならない。
        // 別名を小文字でクォートして固定する。H2 は無引用の識別子を大文字化するため、
        // decoder の field("cart_id") と一致させるにはこうする必要がある。
        Record row = dsl.fetchOne("""
                SELECT c.cart_id AS "cart_id", COALESCE(SUM(ci.quantity), 0) AS "total_quantity"
                FROM cart c
                LEFT JOIN cart_item ci ON ci.cart_id = c.cart_id
                WHERE c.user_id = ?
                GROUP BY c.cart_id
                """, userId.value());
        // 生の Record をドメインの Cart にデコードして返す。ここが「境界で decode」する箇所。
        return JooqCartDecoders.CART.decode(row);
    }

    @Override
    public void addItem(CartId cartId, CartItem item) {
        // ドメインの CartItem を列の Map にエンコードする。ここが「境界で encode」する箇所。
        Map<String, @Nullable Object> row = JooqCartEncoders.CART_ITEM_ROW.encode(item);
        UUID productId = (UUID) row.get("product_id");
        int quantity = (Integer) row.get("quantity");

        // 既存行があれば数量を加算、無ければ挿入。どちらも全ロードはしない。
        int updated = dsl.update(table("cart_item"))
                .set(field("quantity", Integer.class),
                        field("quantity", Integer.class).plus(quantity))
                .where(field("cart_id", UUID.class).eq(cartId.value()))
                .and(field("product_id", UUID.class).eq(productId))
                .execute();
        if (updated == 0) {
            dsl.insertInto(table("cart_item"))
                    .columns(
                            field("cart_item_id"),
                            field("cart_id"),
                            field("product_id"),
                            field("quantity"))
                    .values(UUID.randomUUID(), cartId.value(), productId, quantity)
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
