package com.example.cart.usecase;

import com.example.cart.domain.Cart;
import com.example.cart.domain.CartItem;
import java.util.List;
import java.util.UUID;

/**
 * カートのポート。ユースケースが必要とする I/O の抽象なので、domain ではなくアプリケーション層に置く。
 *
 * <p>{@link #getItemCount} は集計クエリで合計数量を取り（全ロードしない＝高速）、{@link #addItem} は
 * 1商品を直接書き込む。ドメインはこれらを呼ばない。呼ぶのはユースケース。ドメインは純粋なままだが、
 * 不変条件チェックがドメインの外に出る。
 */
public interface CartRepository {
    Cart loadCart(UUID userId);

    int getItemCount(UUID cartId);

    void addItem(UUID cartId, UUID productId, int quantity);

    /** チェックアウト用にカートの全明細を取り出す。 */
    List<CartItem> findAllItems(UUID userId);
}
