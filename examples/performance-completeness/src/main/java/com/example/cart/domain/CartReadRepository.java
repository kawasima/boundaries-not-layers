package com.example.cart.domain;

import java.util.List;
import java.util.UUID;

/**
 * 参照用リポジトリのポート。ページ単位でカートの中身を取り出す。
 */
public interface CartReadRepository {
    Page<CartItem> findCartItemsByPage(UUID userId, int page, int size);

    /** チェックアウト用にカートの全明細を取り出す。 */
    List<CartItem> findAllItems(UUID userId);
}
