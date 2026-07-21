package com.example.cart.infrastructure;

import static net.unit8.raoh.encode.MapEncoders.discriminate;
import static net.unit8.raoh.encode.MapEncoders.object;
import static net.unit8.raoh.encode.MapEncoders.property;
import static net.unit8.raoh.encode.MapEncoders.variant;
import static net.unit8.raoh.encode.ObjectEncoders.int_;
import static net.unit8.raoh.encode.ObjectEncoders.long_;
import static net.unit8.raoh.encode.ObjectEncoders.string;

import com.example.cart.domain.Order;
import com.example.cart.domain.OrderLine;
import com.example.cart.domain.Orderer;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.encode.Encoder;
import org.jspecify.annotations.Nullable;

/**
 * DB 書き込み境界の encoder。注文ヘッダ・明細・注文者を、それぞれ列の {@code Map} に変換する。
 */
public final class JooqOrderEncoders {

    private JooqOrderEncoders() {}

    /** {@link Order} → orders ヘッダ {@code (user_id, subtotal, discount, total)}。order_id は保存側で補う。 */
    public static final Encoder<Order, Map<String, @Nullable Object>> ORDER_ROW = object(
            property("user_id", o -> o.userId().value(), rawUuid()),
            property("subtotal", o -> o.subtotal().amount(), long_()),
            property("discount", o -> o.discount().amount(), long_()),
            property("total", o -> o.total().amount(), long_()));

    /**
     * {@link Orderer}（sealed）→ フラットな列。判別エンコードで {@code orderer_type} を書き、変種ごとに
     * 必要な列だけを出す。個人なら {@code orderer_name}、法人なら {@code orderer_company_name} /
     * {@code orderer_corporate_number}。decode 側の判別と対になる。
     */
    public static final Encoder<Orderer, Map<String, @Nullable Object>> ORDERER_ROW = discriminate("orderer_type",
            variant(Orderer.Individual.class, "individual", object(
                    property("orderer_email", i -> i.email().value(), string()),
                    property("orderer_name", Orderer.Individual::name, string()))),
            variant(Orderer.Corporation.class, "corporation", object(
                    property("orderer_email", c -> c.email().value(), string()),
                    property("orderer_company_name", Orderer.Corporation::companyName, string()),
                    property("orderer_corporate_number", Orderer.Corporation::corporateNumber, string()))));

    /** {@link OrderLine} → order_line 行 {@code (product_id, quantity, unit_price)}。 */
    public static final Encoder<OrderLine, Map<String, @Nullable Object>> ORDER_LINE_ROW = object(
            property("product_id", l -> l.productId().value(), rawUuid()),
            property("quantity", l -> l.quantity().value(), int_()),
            property("unit_price", l -> l.unitPrice().amount(), long_()));

    /** UUID を変換せずそのまま渡すエンコーダ（jOOQ/H2 の UUID 列にバインドするため）。 */
    private static Encoder<UUID, Object> rawUuid() {
        return v -> v;
    }
}
