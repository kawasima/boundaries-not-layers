package com.example.cart.web;

import com.example.cart.domain.Corporation;
import com.example.cart.domain.Individual;
import com.example.cart.domain.Order;
import com.example.cart.domain.OrderLine;
import com.example.cart.domain.Orderer;
import com.example.cart.domain.Quotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 応答境界の encoder。Souther の {@link Order} / {@link Quotation} を public アクセサで開き、
 * 応答 {@code Map} に載せる。金額（小計・割引・合計）はドメインが計算した {@code Charge} の値をそのまま出す。
 */
public final class OrderViewEncoders {

    private OrderViewEncoders() {}

    private static Map<String, Object> lineView(OrderLine line) {
        long unitPrice = line.unitPrice().value();
        long quantity = line.quantity().value();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("productId", line.productId().value());
        m.put("quantity", quantity);
        m.put("unitPrice", unitPrice);
        m.put("subtotal", unitPrice * quantity);
        return m;
    }

    public static Map<String, Object> orderView(Order order) {
        List<Map<String, Object>> lines = order.lines().stream()
                .map(o -> lineView((OrderLine) o))
                .toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", order.id().value());
        body.put("orderer", ordererView(order.orderer()));
        body.put("subtotal", order.charge().subtotal().value());
        body.put("discount", order.charge().discount().value());
        body.put("total", order.charge().total().value());
        body.put("lines", lines);
        return body;
    }

    public static Map<String, Object> quotationView(Quotation quote) {
        List<Map<String, Object>> lines = quote.lines().stream()
                .map(o -> lineView((OrderLine) o))
                .toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("quoteId", quote.id().value());
        body.put("orderer", ordererView(quote.orderer()));
        body.put("subtotal", quote.charge().subtotal().value());
        body.put("discount", quote.charge().discount().value());
        body.put("total", quote.charge().total().value());
        body.put("validUntil", quote.validUntil());
        body.put("lines", lines);
        return body;
    }

    private static Map<String, Object> ordererView(Orderer orderer) {
        Map<String, Object> m = new LinkedHashMap<>();
        switch (orderer) {
            case Individual individual -> {
                m.put("type", "individual");
                m.put("email", individual.email().value());
                m.put("name", individual.name());
            }
            case Corporation corporation -> {
                m.put("type", "corporation");
                m.put("email", corporation.email().value());
                m.put("companyName", corporation.companyName());
                m.put("corporateNumber", corporation.corporateNumber());
            }
            default -> throw new IllegalStateException("unknown orderer: " + orderer);
        }
        return m;
    }
}
