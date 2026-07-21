package com.example.cart.domain;

import net.unit8.raoh.Path;
import net.unit8.raoh.Result;

/**
 * カート。性能・完全性・純粋性のトレードオフを、境界のデコードで解いた版のドメインモデル。
 *
 * <p>この Cart は不変条件の判断に必要な状態だけ——識別子と現在の合計数量——を持つ。
 * 全アイテムはロードしない（性能）。上限チェックは Cart 自身が行う（完全性）。そして
 * {@link #add} はリポジトリを一切受け取らない純関数で、外界とやり取りしない（純粋性）。
 *
 * <p>{@link #add} は「上限内なら追加する item を作る」という単一責務の振る舞い。成否は
 * 例外ではなく {@link Result} で返すので、他の振る舞いと {@code flatMap} で合成できる。
 */
public record Cart(CartId id, TotalQuantity currentQuantity) {

    /** カートに入れられる合計数量の上限。 */
    public static final int UPPER_BOUND = 10000;

    /**
     * 上限を超えなければ、追加する {@link CartItem} を Ok で返す。超えるなら Err を返す。
     * I/O は行わない純粋な振る舞い。
     */
    public Result<CartItem> add(ProductId productId, Quantity quantity) {
        if (!currentQuantity.canAdd(quantity, UPPER_BOUND)) {
            return Result.fail(Path.ROOT, "cart_full", "商品数の上限に達しています");
        }
        return Result.ok(new CartItem(productId, quantity));
    }
}
