package com.example.cart.domain;

public class CartEmptyException extends RuntimeException {
    public CartEmptyException() {
        super("カートが空です");
    }
}
