package com.example.cart.domain;

/**
 * 金額（円）。整数の円で持つ値オブジェクト。金額に関する計算はこの型に閉じる。
 */
public record Money(long amount) {

    public static final Money ZERO = new Money(0);

    public Money plus(Money other) {
        return new Money(amount + other.amount);
    }

    public Money minus(Money other) {
        return new Money(amount - other.amount);
    }

    public Money times(int quantity) {
        return new Money(amount * quantity);
    }

    /** 指定パーセントの金額（端数切り捨て）。 */
    public Money percent(int rate) {
        return new Money(amount * rate / 100);
    }

    public boolean isAtLeast(Money threshold) {
        return amount >= threshold.amount;
    }
}
