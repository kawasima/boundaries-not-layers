package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.CartItem;
import com.example.cart.domain.CartQueryRepository;
import com.example.cart.domain.Page;
import com.example.cart.domain.UserId;
import java.util.List;
import java.util.UUID;
import net.unit8.raoh.decode.Decoder;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * 参照用リポジトリ（raoh 版）。指定ページ分だけを LIMIT/OFFSET で取り出し、
 * 各行を {@link JooqCartDecoders#CART_ITEM} でデコードする。
 *
 * <p>{@code CART_ITEM.list()} が「1行 decoder」を「行リスト decoder」へ持ち上げ、全行のエラーを
 * 蓄積しつつまとめてデコードする。RowMapper も ORM も要らない——境界で raoh がデコードする。
 */
@Repository
public class JooqCartQueryRepository implements CartQueryRepository {

    private final DSLContext dsl;

    public JooqCartQueryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Page<CartItem> findItemsByPage(UserId userId, int page, int size) {
        UUID cartId = dsl.select(field("cart_id", UUID.class))
                .from(table("cart"))
                .where(field("user_id", UUID.class).eq(userId.value()))
                .fetchOne(field("cart_id", UUID.class));
        if (cartId == null) {
            return new Page<>(0, page, size, List.of());
        }

        long total = dsl.select(count())
                .from(table("cart_item"))
                .where(field("cart_id", UUID.class).eq(cartId))
                .fetchOne(0, long.class);

        // 別名を小文字でクォートして固定（H2 の無引用識別子は大文字化されるため）。
        org.jooq.Result<Record> rows = dsl.fetch("""
                SELECT product_id AS "product_id", quantity AS "quantity"
                FROM cart_item
                WHERE cart_id = ?
                ORDER BY product_id
                LIMIT ? OFFSET ?
                """, cartId, size, (long) page * size);

        // list() で行リストをまとめてデコード。org.jooq.Result は List<Record> なのでそのまま渡せる。
        Decoder<List<Record>, List<CartItem>> rowsDecoder = JooqCartDecoders.CART_ITEM.list();
        List<CartItem> items = rowsDecoder.decode(rows).getOrThrow();

        return new Page<>(total, page, size, items);
    }

    @Override
    public List<CartItem> findAllItems(UserId userId) {
        UUID cartId = dsl.select(field("cart_id", UUID.class))
                .from(table("cart"))
                .where(field("user_id", UUID.class).eq(userId.value()))
                .fetchOne(field("cart_id", UUID.class));
        if (cartId == null) {
            return List.of();
        }
        org.jooq.Result<Record> rows = dsl.fetch("""
                SELECT product_id AS "product_id", quantity AS "quantity"
                FROM cart_item
                WHERE cart_id = ?
                ORDER BY product_id
                """, cartId);
        return JooqCartDecoders.CART_ITEM.list().decode(rows).getOrThrow();
    }
}
