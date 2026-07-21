package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.sum;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.Cart;
import com.example.cart.domain.CartItem;
import com.example.cart.usecase.CartRepository;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * 性能+純粋性版のカートリポジトリ。
 *
 * <p>{@link #getItemCount} は SUM の集計クエリ、{@link #addItem} は直接の upsert。どちらも
 * 全アイテムをロードしない（＝性能を守る）。不変条件の判定はここには無く、ユースケースにある。
 */
@Repository
public class JooqCartRepository implements CartRepository {

    private final DSLContext dsl;

    public JooqCartRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Cart loadCart(UUID userId) {
        // anemic な Cart を、識別子だけで組み立てて返す。アイテムはロードしない。
        return new Cart(resolveCartId(userId));
    }

    @Override
    public int getItemCount(UUID cartId) {
        Integer total = dsl.select(sum(field("quantity", Integer.class)))
                .from(table("cart_item"))
                .where(field("cart_id", UUID.class).eq(cartId))
                .fetchOne(0, Integer.class);
        return total == null ? 0 : total;
    }

    @Override
    public void addItem(UUID cartId, UUID productId, int quantity) {
        int updated = dsl.update(table("cart_item"))
                .set(field("quantity", Integer.class),
                        field("quantity", Integer.class).plus(quantity))
                .where(field("cart_id", UUID.class).eq(cartId))
                .and(field("product_id", UUID.class).eq(productId))
                .execute();
        if (updated == 0) {
            dsl.insertInto(table("cart_item"))
                    .columns(
                            field("cart_item_id"),
                            field("cart_id"),
                            field("product_id"),
                            field("quantity"))
                    .values(UUID.randomUUID(), cartId, productId, quantity)
                    .execute();
        }
    }

    @Override
    public List<CartItem> findAllItems(UUID userId) {
        UUID cartId = dsl.select(field("cart_id", UUID.class))
                .from(table("cart"))
                .where(field("user_id", UUID.class).eq(userId))
                .fetchOne(field("cart_id", UUID.class));
        if (cartId == null) {
            return List.of();
        }
        return dsl.select(field("product_id", UUID.class), field("quantity", Integer.class))
                .from(table("cart_item"))
                .where(field("cart_id", UUID.class).eq(cartId))
                .orderBy(field("product_id"))
                .fetch(r -> new CartItem(r.get("product_id", UUID.class), r.get("quantity", Integer.class)));
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
