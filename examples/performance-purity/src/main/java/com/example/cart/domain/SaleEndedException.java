package com.example.cart.domain;

public class SaleEndedException extends RuntimeException {
    public SaleEndedException() {
        super("販売が終了しました");
    }
}
