package com.example.cart.usecase;

public record AddItemToCartCommand(String userId, String productId, int quantity) {
}
