package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.CartItem;
import com.example.cart.domain.UserId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.Path;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * 参照用リポジトリ。ページ分／全件の cart_item 行を取り、各行を {@code CartItem.decoder()} で
 * Souther の {@link CartItem} にデコードする。
 */
@Repository
public class CartQueryRepository {

    private final DSLContext dsl;

    public CartQueryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Page<CartItem> findItemsByPage(UserId userId, int page, int size) {
        UUID cartId = cartIdOf(userId);
        if (cartId == null) {
            return new Page<>(0, page, size, List.of());
        }
        long total = dsl.select(count())
                .from(table("cart_item"))
                .where(field("cart_id", UUID.class).eq(cartId))
                .fetchOne(0, long.class);

        org.jooq.Result<Record> rows = dsl.fetch("""
                SELECT product_id, quantity
                FROM cart_item
                WHERE cart_id = ?
                ORDER BY product_id
                LIMIT ? OFFSET ?
                """, cartId, size, (long) page * size);

        return new Page<>(total, page, size, toItems(rows));
    }

    public List<CartItem> findAllItems(UserId userId) {
        UUID cartId = cartIdOf(userId);
        if (cartId == null) {
            return List.of();
        }
        org.jooq.Result<Record> rows = dsl.fetch("""
                SELECT product_id, quantity
                FROM cart_item
                WHERE cart_id = ?
                ORDER BY product_id
                """, cartId);
        return toItems(rows);
    }

    private UUID cartIdOf(UserId userId) {
        return dsl.select(field("cart_id", UUID.class))
                .from(table("cart"))
                .where(field("user_id", UUID.class).eq(UUID.fromString(userId.value())))
                .fetchOne(field("cart_id", UUID.class));
    }

    private static List<CartItem> toItems(org.jooq.Result<Record> rows) {
        List<CartItem> items = new ArrayList<>(rows.size());
        for (Record row : rows) {
            UUID productId = row.get(0, UUID.class);
            int quantity = row.get(1, Integer.class);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productId", productId.toString());
            m.put("quantity", (long) quantity);
            items.add(CartItem.decoder().decode(m, Path.ROOT).getOrThrow());
        }
        return items;
    }
}
