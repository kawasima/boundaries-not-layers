package com.example.cart.domain;

/** カートに入る1商品と、その数量。 */
public record CartItem(ProductId productId, Quantity quantity) {
}
