package com.example.cart.domain;

/**
 * 金額（円）。値オブジェクトは用意した——が、加算や割引といった計算はこの型に持たせていない。
 * 実際の計算は {@link CheckoutService} が long のまま行い、最後にこの型に詰め直すだけになっている。
 */
public record Money(long amount) {
}
