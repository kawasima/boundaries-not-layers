package com.example.cart.usecase;

import com.example.cart.domain.Order;

/**
 * 注文永続化のポート。ユースケースが必要とする I/O の抽象なので、domain ではなくアプリケーション層に置く。
 */
public interface OrderRepository {
    void save(Order order);
}
