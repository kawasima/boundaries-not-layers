package com.example.cart.domain;

public class CartFullException extends RuntimeException {
    public CartFullException() {
        super("商品数の上限に達しています");
    }
}
