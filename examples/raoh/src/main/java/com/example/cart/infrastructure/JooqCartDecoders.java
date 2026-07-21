package com.example.cart.infrastructure;

import static net.unit8.raoh.decode.ObjectDecoders.int_;
import static net.unit8.raoh.jooq.JooqRecordDecoders.combine;
import static net.unit8.raoh.jooq.JooqRecordDecoders.field;

import com.example.cart.domain.Cart;
import com.example.cart.domain.CartId;
import com.example.cart.domain.CartItem;
import com.example.cart.domain.ProductId;
import com.example.cart.domain.Quantity;
import com.example.cart.domain.TotalQuantity;
import net.unit8.raoh.decode.Decoder;
import org.jooq.Record;

/**
 * DB 読み取り境界（jOOQ {@link Record}）の decoder。
 *
 * <p>この設計の要。更新用の {@link #CART} は「集計クエリ1本」の結果行——
 * {@code (cart_id, total_quantity)}——を、合計数量まで持つ完全な {@link Cart} にデコードする。
 * 全アイテムをロードしない（性能）まま、不変条件の判断に必要な状態を丸ごと型に載せる（完全性）。
 * Cart はこの decoder 経由でしか作られないので、不正な状態の Cart は存在し得ない。
 */
public final class JooqCartDecoders {

    private JooqCartDecoders() {}

    /** 集計行 {@code (cart_id, total_quantity)} → {@link Cart}。 */
    public static final Decoder<Record, Cart> CART = combine(
            field("cart_id", JdbcColumns.uuid()).map(CartId::new),
            // 合計数量は 0 以上。SUM の結果（Long/BigDecimal でも）int_() が Number として受ける。
            field("total_quantity", int_().nonNegative()).map(TotalQuantity::new)
    ).map(Cart::new);

    /** 明細行 {@code (product_id, quantity)} → {@link CartItem}。一覧では {@code CART_ITEM.list()} で使う。 */
    public static final Decoder<Record, CartItem> CART_ITEM = combine(
            field("product_id", JdbcColumns.uuid()).map(ProductId::new),
            field("quantity", int_().positive()).map(Quantity::new)
    ).map(CartItem::new);
}
