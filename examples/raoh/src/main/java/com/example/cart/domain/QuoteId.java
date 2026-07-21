package com.example.cart.domain;

import java.util.UUID;

/** 見積の識別子。 */
public record QuoteId(UUID value) {

    public static QuoteId newId() {
        return new QuoteId(UUID.randomUUID());
    }
}
