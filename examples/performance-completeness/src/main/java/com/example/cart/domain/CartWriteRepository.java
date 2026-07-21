package com.example.cart.domain;

import java.util.UUID;

/**
 * 更新用リポジトリのポート。
 *
 * <p>{@link CartForUpdate} はこのインターフェース越しに、合計数量の取得（{@link #getItemCount}）と
 * 追加（{@link #addItem}）を行う。ドメインがこのポートに依存している点が、この設計の
 * 「純粋性を犠牲にしている」ところ。
 */
public interface CartWriteRepository {
    CartForUpdate loadCart(UUID userId);

    /** カート内の合計数量。全アイテムをロードせず集計クエリで取る（＝性能を守る箇所）。 */
    int getItemCount(UUID cartId);

    /** 1商品を追加する。既にあれば数量を合算する。 */
    void addItem(UUID cartId, CartItem item);
}
