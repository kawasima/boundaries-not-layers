package com.example.cart.domain;

import java.util.UUID;

/** 注文の識別子。 */
public record OrderId(UUID value) {

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }
}
