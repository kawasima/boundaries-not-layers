package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.CartItem;
import com.example.cart.domain.ItemAdded;
import com.example.cart.domain.PendingItem;
import com.example.cart.domain.SaveItem;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.Path;
import org.jooq.DSLContext;

/**
 * {@code saveItem} INJECTED 振る舞いの jOOQ 実装。{@link PendingItem} からカートと明細を取り出し、
 * 既存行があれば数量を加算、無ければ挿入する。書き込んだ内容を Souther の {@link ItemAdded} に組んで返す。
 */
public final class JooqSaveItem extends SaveItem {

    private final DSLContext dsl;

    public JooqSaveItem(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public ItemAdded apply(PendingItem pending) {
        UUID cartId = UUID.fromString(pending.cart().id().value());
        CartItem item = pending.item();
        UUID productId = UUID.fromString(item.productId().value());
        int quantity = (int) item.quantity().value();

        int updated = dsl.update(table("cart_item"))
                .set(field("quantity", Integer.class), field("quantity", Integer.class).plus(quantity))
                .where(field("cart_id", UUID.class).eq(cartId))
                .and(field("product_id", UUID.class).eq(productId))
                .execute();
        if (updated == 0) {
            dsl.insertInto(table("cart_item"))
                    .columns(field("cart_item_id"), field("cart_id"), field("product_id"), field("quantity"))
                    .values(UUID.randomUUID(), cartId, productId, quantity)
                    .execute();
        }

        return ItemAdded.decoder().decode(Map.of(
                "productId", item.productId().value(),
                "quantity", item.quantity().value()), Path.ROOT).getOrThrow();
    }
}
