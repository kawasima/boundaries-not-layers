package com.example.cart.domain;

import java.util.UUID;

public interface ProductRepository {
    boolean isNowOnSale(UUID productId);

    /** 商品の単価（円）。 */
    long priceOf(UUID productId);
}
