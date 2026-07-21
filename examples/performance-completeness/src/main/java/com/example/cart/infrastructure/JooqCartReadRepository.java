package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.CartItem;
import com.example.cart.domain.CartReadRepository;
import com.example.cart.domain.Page;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * 性能+完全性版の参照用リポジトリ。指定ページ分だけを LIMIT/OFFSET で取り出す。
 */
@Repository
public class JooqCartReadRepository implements CartReadRepository {

    private final DSLContext dsl;

    public JooqCartReadRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Page<CartItem> findCartItemsByPage(UUID userId, int page, int size) {
        UUID cartId = dsl.select(field("cart_id", UUID.class))
                .from(table("cart"))
                .where(field("user_id", UUID.class).eq(userId))
                .fetchOne(field("cart_id", UUID.class));
        if (cartId == null) {
            return new Page<>(0, page, size, List.of());
        }

        long total = dsl.select(count())
                .from(table("cart_item"))
                .where(field("cart_id", UUID.class).eq(cartId))
                .fetchOne(0, long.class);

        List<CartItem> items = dsl.select(
                        field("product_id", UUID.class),
                        field("quantity", Integer.class))
                .from(table("cart_item"))
                .where(field("cart_id", UUID.class).eq(cartId))
                .orderBy(field("product_id"))
                .limit(size)
                .offset((long) page * size)
                .fetch(r -> new CartItem(
                        r.get("product_id", UUID.class),
                        r.get("quantity", Integer.class)));

        return new Page<>(total, page, size, items);
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
}
