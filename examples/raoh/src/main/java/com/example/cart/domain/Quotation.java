package com.example.cart.domain;

import java.time.LocalDate;
import java.util.List;
import net.unit8.raoh.Path;
import net.unit8.raoh.Result;

/**
 * 見積書。注文の亜種。金額計算は注文とまったく同じ（{@link Charge}）で、違うのは終端だけ——確定注文では
 * なく、有効期限つきの見積として返す。永続化はしない。
 *
 * <p>見積の注文者は {@link Orderer.Corporation} に限られる（法人限定）。sealed のおかげで、それを
 * 引数の型で表せる——個人が紛れ込む余地が型として無い。
 */
public record Quotation(
        QuoteId id,
        UserId userId,
        Orderer.Corporation orderer,
        List<OrderLine> lines,
        Charge charge,
        LocalDate validUntil) {

    public Quotation {
        lines = List.copyOf(lines);
    }

    /** 明細・割引方針・有効期限から見積を組む。明細が空なら業務エラーを返す。 */
    public static Result<Quotation> issue(
            QuoteId id, UserId userId, Orderer.Corporation orderer,
            List<OrderLine> lines, DiscountPolicy policy, LocalDate validUntil) {
        if (lines.isEmpty()) {
            return Result.fail(Path.ROOT, "empty_cart", "カートが空です");
        }
        return Result.ok(new Quotation(id, userId, orderer, lines, Charge.of(lines, policy), validUntil));
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
