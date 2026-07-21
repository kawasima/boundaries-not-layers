package com.example.cart.domain;

/**
 * 割引の決め方。小計に対して割引額を返す、ドメインの方針（値オブジェクト）。
 *
 * <p>割引は「小計に依存し、どの明細にも Product にも属さない」判断なので、明細やエンティティには
 * 置けない。かといってチェックアウトのサービスに if を書き散らすのでもなく、方針そのものを型にする。
 */
@FunctionalInterface
public interface DiscountPolicy {

    Money discountFor(Money subtotal);

    /** 標準の割引: 小計が閾値以上なら指定率を割り引く。 */
    static DiscountPolicy standard() {
        return thresholdRate(new Money(5000), 10);
    }

    /** 小計が {@code threshold} 以上のとき {@code rate}% を割り引く方針。 */
    static DiscountPolicy thresholdRate(Money threshold, int rate) {
        return subtotal -> subtotal.isAtLeast(threshold) ? subtotal.percent(rate) : Money.ZERO;
    }
}
