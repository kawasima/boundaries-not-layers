package com.example.cart.domain;

import java.util.List;
import java.util.UUID;

/**
 * 注文。注文者・明細・小計・割引・合計を全部フィールドに持つが、それらを計算するのは Order ではなく
 * {@link CheckoutService}。ここは値の入れ物で、{@code total()} のような振る舞いは無い。
 */
public record Order(
        UUID orderId,
        UUID userId,
        Orderer orderer,
        List<OrderLine> lines,
        Money subtotal,
        Money discount,
        Money total) {
}
