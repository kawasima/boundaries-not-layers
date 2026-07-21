package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.Cart;
import com.example.cart.domain.CartItem;
import com.example.cart.usecase.CartRepository;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * 純粋性+完全性版のカートリポジトリ。
 *
 * <p>{@link #loadCart(UUID)} が cart_item を全件 SELECT して {@link Cart} を組み立てる。
 * ドメインが完全な状態を持てる代わりに、アイテムが増えるほど重くなる。ここが
 * トリレンマで犠牲にしている「性能」。
 */
@Repository
public class JooqCartRepository implements CartRepository {

    private final DSLContext dsl;

    public JooqCartRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Cart loadCart(UUID userId) {
        UUID cartId = resolveCartId(userId);

        // 全アイテムをロードする（＝性能を犠牲にしている箇所）。
        List<CartItem> items = dsl.select(
                        field("product_id", UUID.class),
                        field("quantity", Integer.class))
                .from(table("cart_item"))
                .where(field("cart_id", UUID.class).eq(cartId))
                .fetch(r -> new CartItem(
                        r.get("product_id", UUID.class),
                        r.get("quantity", Integer.class)));

        return new Cart(items);
    }

    @Override
    public void saveCart(UUID userId, Cart cart) {
        UUID cartId = resolveCartId(userId);

        // Cart はメモリ上に全アイテムを持っているので、まるごと入れ替える。
        dsl.deleteFrom(table("cart_item"))
                .where(field("cart_id", UUID.class).eq(cartId))
                .execute();

        for (CartItem item : cart.items()) {
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
