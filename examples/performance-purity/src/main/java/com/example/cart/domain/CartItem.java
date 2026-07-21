package com.example.cart.domain;

import java.util.UUID;

/**
 * カートに入っている1商品とその数量。
 */
public record CartItem(UUID productId, int quantity) {
}
