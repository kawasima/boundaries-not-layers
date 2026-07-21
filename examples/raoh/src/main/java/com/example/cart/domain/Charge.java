package com.example.cart.domain;

import java.util.List;

/**
 * 明細と割引方針から決まる金額（小計・割引・合計）。注文と見積で同じ計算を使うので、ここに1つ置く。
 * 亜種が増えても金額計算はこの1箇所を再利用すればよい。
 */
public record Charge(Money subtotal, Money discount, Money total) {

    public static Charge of(List<OrderLine> lines, DiscountPolicy policy) {
        Money subtotal = Money.ZERO;
        for (OrderLine line : lines) {
            subtotal = subtotal.plus(line.subtotal());
        }
        Money discount = policy.discountFor(subtotal);
        return new Charge(subtotal, discount, subtotal.minus(discount));
    }
}
