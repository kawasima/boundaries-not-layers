package com.example.cart.infrastructure;

import static net.unit8.raoh.decode.ObjectDecoders.bool;
import static net.unit8.raoh.decode.ObjectDecoders.long_;
import static net.unit8.raoh.jooq.JooqRecordDecoders.combine;
import static net.unit8.raoh.jooq.JooqRecordDecoders.field;

import com.example.cart.domain.Money;
import com.example.cart.domain.Product;
import com.example.cart.domain.ProductId;
import net.unit8.raoh.decode.Decoder;
import org.jooq.Record;

/**
 * DB 読み取り境界（jOOQ {@link Record}）の商品 decoder。
 *
 * <p>{@code (product_id, on_sale, price)} の行を、販売状態と単価まで持つ {@link Product} にデコードする。
 */
public final class JooqProductDecoders {

    private JooqProductDecoders() {}

    /** 商品行 {@code (product_id, on_sale, price)} → {@link Product}。 */
    public static final Decoder<Record, Product> PRODUCT = combine(
            field("product_id", JdbcColumns.uuid()).map(ProductId::new),
            field("on_sale", bool()),
            field("price", long_().map(Money::new))
    ).map(Product::new);
}
