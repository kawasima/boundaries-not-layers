package com.example.cart.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 見積書。注文と同じく、小計・割引・合計を全部フィールドに持つ入れ物。金額を計算するのは
 * {@link CheckoutService}。注文と見積で同じ計算だが、置き場がドメインに無いのでサービス側で二度書かれる。
 */
public record Quotation(
        UUID quoteId,
        UUID userId,
        Orderer orderer,
        List<OrderLine> lines,
        Money subtotal,
        Money discount,
        Money total,
        LocalDate validUntil) {
}
