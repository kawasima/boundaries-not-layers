package com.example.cart.domain;

import java.util.List;

/** 参照用リポジトリのポート。ページ単位の参照と、チェックアウト用の全件取得。 */
public interface CartQueryRepository {

    Page<CartItem> findItemsByPage(UserId userId, int page, int size);

    /** カートの全明細。チェックアウトはページングせず全件を注文にする。 */
    List<CartItem> findAllItems(UserId userId);
}
