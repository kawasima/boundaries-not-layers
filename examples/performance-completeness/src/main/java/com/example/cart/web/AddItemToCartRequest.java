package com.example.cart.web;

public record AddItemToCartRequest(String userId, String productId, int quantity) {
}
