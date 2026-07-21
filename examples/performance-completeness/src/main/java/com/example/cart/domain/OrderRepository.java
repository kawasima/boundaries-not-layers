package com.example.cart.domain;

/**
 * 注文永続化のポート。この例は純粋性を犠牲にする側なので、ドメイン（{@link CheckoutService}）が
 * このポートを直接呼ぶ。
 */
public interface OrderRepository {
    void save(Order order);
}
