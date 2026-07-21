package com.example.cart.domain;

/** 注文永続化の gateway。 */
public interface OrderRepository {
    void save(Order order);
}
