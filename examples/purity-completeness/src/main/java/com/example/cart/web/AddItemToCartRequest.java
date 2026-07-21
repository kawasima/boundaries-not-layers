package com.example.cart.web;

/**
 * POST /carts/items のリクエストボディ。
 */
public record AddItemToCartRequest(String userId, String productId, int quantity) {
}
