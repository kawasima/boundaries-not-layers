package com.example.cart.domain;

import net.unit8.raoh.Result;

/**
 * 更新用リポジトリのポート（永続化の gateway）。
 *
 * <p>{@link #loadForUpdate} は集計クエリ1本の結果をデコードして、合計数量を持つ完全な
 * {@link Cart} を {@link Result} で返す。ドメイン（{@link Cart#add}）はこのポートに依存しない——
 * 上限チェックはロード済みの Cart 上で純粋に行われる。
 */
public interface CartRepository {

    /** 合計数量まで含めてデコード済みの Cart を返す。全アイテムはロードしない。 */
    Result<Cart> loadForUpdate(UserId userId);

    /** 1商品を追加する。既にあれば数量を合算する。 */
    void addItem(CartId cartId, CartItem item);
}
