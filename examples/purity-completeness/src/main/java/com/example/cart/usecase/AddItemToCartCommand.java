package com.example.cart.usecase;

/**
 * 「カートに商品を入れる」ユースケースへの入力。境界の外側から来る生の値。
 */
public record AddItemToCartCommand(String userId, String productId, int quantity) {
}
