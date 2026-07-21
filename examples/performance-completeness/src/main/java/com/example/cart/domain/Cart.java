package com.example.cart.domain;

import java.util.UUID;

/**
 * 参照用の Cart。更新用（{@link CartForUpdate}）と役割を分けている。
 *
 * <p>参照時も全アイテムを一度に持たず、必要なページだけをリポジトリから取り出す。
 */
public class Cart {

    private final UUID userId;

    public Cart(UUID userId) {
        this.userId = userId;
    }

    public Page<CartItem> items(int page, int size, CartReadRepository cartRepository) {
        return cartRepository.findCartItemsByPage(userId, page, size);
    }
}
