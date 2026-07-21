package com.example.cart.domain;

import java.util.UUID;

/**
 * 性能+純粋性を両立させた Cart。完全性を犠牲にしている。
 *
 * <p>ドメインは外界（DB）とやり取りしない（純粋）。全アイテムのロードもしない（高速）。
 * その代償として、Cart は識別子と上限値の定数を持つだけの anemic なオブジェクトになり、
 * 「合計数量が上限を超えないか」という自分自身の不変条件を、自分では判断できない。
 *
 * <p>不変条件の判断はユースケース側へ漏れ出す。ここで犠牲になっているのは「完全性」。
 */
public class Cart {

    /** カートに入れられる合計数量の上限。定数は持つが、判定はここでは行わない。 */
    public static final int UPPER_BOUND = 10000;

    private final UUID id;

    public Cart(UUID id) {
        this.id = id;
    }

    public UUID id() {
        return id;
    }
}
