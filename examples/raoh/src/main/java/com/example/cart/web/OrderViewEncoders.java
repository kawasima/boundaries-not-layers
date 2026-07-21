package com.example.cart.web;

import static net.unit8.raoh.encode.MapEncoders.object;
import static net.unit8.raoh.encode.MapEncoders.property;
import static net.unit8.raoh.encode.ObjectEncoders.int_;
import static net.unit8.raoh.encode.ObjectEncoders.long_;
import static net.unit8.raoh.encode.ObjectEncoders.uuid;

import com.example.cart.domain.Order;
import com.example.cart.domain.OrderLine;
import com.example.cart.domain.Orderer;
import com.example.cart.domain.Quotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.unit8.raoh.encode.Encoder;
import org.jspecify.annotations.Nullable;

/**
 * HTTP 応答境界の encoder。注文を JSON になる {@code Map} へ変換する。金額はドメインが計算した値を
 * そのまま載せるだけ（合計・割引は {@link Order} の責務で、ここでは計算しない）。
 */
public final class OrderViewEncoders {

    private OrderViewEncoders() {}

    /** {@link OrderLine} → {@code {"productId","quantity","unitPrice","subtotal"}}。 */
    public static final Encoder<OrderLine, Map<String, @Nullable Object>> LINE_VIEW = object(
            property("productId", l -> l.productId().value(), uuid()),
            property("quantity", l -> l.quantity().value(), int_()),
            property("unitPrice", l -> l.unitPrice().amount(), long_()),
            property("subtotal", l -> l.subtotal().amount(), long_()));

    /** {@link Order} を応答 {@code Map}（orderId, subtotal, discount, total, lines[]）にする。 */
    public static Map<String, @Nullable Object> orderView(Order order) {
        List<Map<String, @Nullable Object>> lines = order.lines().stream()
                .map(LINE_VIEW::encode)
                .toList();
        var body = new LinkedHashMap<String, @Nullable Object>();
        body.put("orderId", order.id().value().toString());
        body.put("orderer", ordererView(order.orderer()));
        body.put("subtotal", order.subtotal().amount());
        body.put("discount", order.discount().amount());
        body.put("total", order.total().amount());
        body.put("lines", lines);
        return body;
    }

    /** {@link Quotation} を応答 {@code Map} にする。金額は注文と同じ、加えて有効期限を持つ。 */
    public static Map<String, @Nullable Object> quotationView(Quotation quote) {
        List<Map<String, @Nullable Object>> lines = quote.lines().stream()
                .map(LINE_VIEW::encode)
                .toList();
        var body = new LinkedHashMap<String, @Nullable Object>();
        body.put("quoteId", quote.id().value().toString());
        body.put("orderer", ordererView(quote.orderer()));
        body.put("subtotal", quote.subtotal().amount());
        body.put("discount", quote.discount().amount());
        body.put("total", quote.total().amount());
        body.put("validUntil", quote.validUntil().toString());
        body.put("lines", lines);
        return body;
    }

    /** 注文者を応答 {@code Map} にする。sealed なので switch が個人／法人の網羅を保証する。 */
    private static Map<String, @Nullable Object> ordererView(Orderer orderer) {
        return switch (orderer) {
            case Orderer.Individual(var email, var name) -> Map.of(
                    "type", "individual", "email", email.value(), "name", name);
            case Orderer.Corporation(var email, var companyName, var corporateNumber) -> Map.of(
                    "type", "corporation", "email", email.value(),
                    "companyName", companyName, "corporateNumber", corporateNumber);
        };
    }
}

