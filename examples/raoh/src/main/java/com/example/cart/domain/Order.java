package com.example.cart.domain;

import java.util.List;
import net.unit8.raoh.Path;
import net.unit8.raoh.Result;

/**
 * 注文。注文者・明細・金額（{@link Charge}）を持つ集約。金額計算は {@link Charge} に委ねるので、
 * 注文と見積で同じ計算を共有できる。{@link #place} は空カートを弾く業務ルールを持つファクトリ。
 */
public record Order(OrderId id, UserId userId, Orderer orderer, List<OrderLine> lines, Charge charge) {

    public Order {
        lines = List.copyOf(lines);
    }

    /** 注文者・明細・割引方針から注文を組む。明細が空なら業務エラーを返す。 */
    public static Result<Order> place(
            OrderId id, UserId userId, Orderer orderer, List<OrderLine> lines, DiscountPolicy policy) {
        if (lines.isEmpty()) {
            return Result.fail(Path.ROOT, "empty_cart", "カートが空です");
        }
        return Result.ok(new Order(id, userId, orderer, lines, Charge.of(lines, policy)));
    }

    public Money subtotal() {
        return charge.subtotal();
    }

    public Money discount() {
        return charge.discount();
    }

    public Money total() {
        return charge.total();
    }
}
