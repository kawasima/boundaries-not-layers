package com.example.cart.domain;

/**
 * カートに現在入っている合計数量。全アイテムをロードせず、集計クエリ1本の結果を
 * デコードして得る。0以上であることは境界の decoder（{@code int_().nonNegative()}）が保証する。
 */
public record TotalQuantity(int value) {

    /** この合計に {@code quantity} を足しても上限 {@code upperBound} を超えないか。 */
    public boolean canAdd(Quantity quantity, int upperBound) {
        return value + quantity.value() <= upperBound;
    }
}
