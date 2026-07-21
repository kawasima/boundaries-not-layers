package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.sum;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.CartForUpdate;
import com.example.cart.domain.CartItem;
import com.example.cart.domain.CartWriteRepository;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * 性能+完全性版の更新用リポジトリ。
 *
 * <p>{@link #getItemCount} は SUM の集計クエリ1本で合計数量を取る。全アイテムをロードしない
 * ので、アイテムが増えても更新は重くならない（＝性能を守っている箇所）。
 */
@Repository
public class JooqCartWriteRepository implements CartWriteRepository {

    private final DSLContext dsl;

    public JooqCartWriteRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public CartForUpdate loadCart(UUID userId) {
        // アイテムはロードしない。カートの識別子だけを解決して返す。
        return new CartForUpdate(resolveCartId(userId));
    }

    @Override
    public int getItemCount(UUID cartId) {
        // 全アイテムをロードせず、SUM の集計クエリ1本で合計数量を取る。
        // 該当行が無ければ SUM は NULL を返すので Java 側で 0 に丸める。
        Integer total = dsl.select(sum(field("quantity", Integer.class)))
                .from(table("cart_item"))
                .where(field("cart_id", UUID.class).eq(cartId))
                .fetchOne(0, Integer.class);
        return total == null ? 0 : total;
    }

    @Override
    public void addItem(UUID cartId, CartItem item) {
        // 既存行があれば数量を加算、無ければ挿入。ここでも全ロードはしない。
        int updated = dsl.update(table("cart_item"))
                .set(field("quantity", Integer.class),
                        field("quantity", Integer.class).plus(item.quantity()))
                .where(field("cart_id", UUID.class).eq(cartId))
                .and(field("product_id", UUID.class).eq(item.productId()))
                .execute();
        if (updated == 0) {
            dsl.insertInto(table("cart_item"))
                    .columns(
                            field("cart_item_id"),
                            field("cart_id"),
                            field("product_id"),
                            field("quantity"))
                    .values(UUID.randomUUID(), cartId, item.productId(), item.quantity())
                    .execute();
        }
    }

    private UUID resolveCartId(UUID userId) {
        UUID cartId = dsl.select(field("cart_id", UUID.class))
                .from(table("cart"))
                .where(field("user_id", UUID.class).eq(userId))
                .fetchOne(field("cart_id", UUID.class));
        if (cartId == null) {
            cartId = UUID.randomUUID();
            dsl.insertInto(table("cart"))
                    .columns(field("cart_id"), field("user_id"))
                    .values(cartId, userId)
                    .execute();
        }
        return cartId;
    }
}
