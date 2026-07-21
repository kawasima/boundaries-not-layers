package com.example.cart.domain;

import java.time.LocalDate;
import java.util.function.BiFunction;
import net.unit8.raoh.Result;
import org.springframework.stereotype.Component;

/**
 * 「見積書を発行する」ドメインの振る舞い。注文（{@link PlaceOrder}）の亜種。
 *
 * <p>{@link PlaceOrder} と同じ共有の {@link PriceCart} を再利用し、終端だけ {@link Quotation} に差し替える。
 * {@link PlaceOrder} には一切手を入れていない。亜種の追加は「小さい振る舞いを足して合成し直す」だけで済む。
 *
 * <p>引数が {@link Orderer.Corporation} なので、見積が法人限定であることが型に出ている。個人を渡す経路は
 * 型として存在しない（弾くのは境界の switch）。
 */
@Component
public class IssueQuote implements BiFunction<UserId, Orderer.Corporation, Result<Quotation>> {

    /** 見積の有効期限（発行日から30日）。 */
    private static final int VALIDITY_DAYS = 30;

    private final PriceCart priceCart;
    private final DiscountPolicy discountPolicy = DiscountPolicy.standard();

    public IssueQuote(PriceCart priceCart) {
        this.priceCart = priceCart;
    }

    @Override
    public Result<Quotation> apply(UserId userId, Orderer.Corporation orderer) {
        LocalDate validUntil = LocalDate.now().plusDays(VALIDITY_DAYS);
        return priceCart.apply(userId)                                             // 共有: 価格付け（PlaceOrder と同じ）
                .flatMap(lines -> Quotation.issue(                                 // 終端: 見積を組む（保存しない）
                        QuoteId.newId(), userId, orderer, lines, discountPolicy, validUntil));
    }
}
