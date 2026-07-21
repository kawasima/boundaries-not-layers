package com.example.cart.usecase;

import java.util.UUID;

/**
 * 商品参照のポート。ユースケースが必要とする I/O の抽象なので、domain ではなくアプリケーション層に置く。
 */
public interface ProductRepository {
    boolean isNowOnSale(UUID productId);

    /** 商品の単価（円）。 */
    long priceOf(UUID productId);
}
