package com.example.cart.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /carts/checkout のリクエストボディ。注文者情報が丸ごと飛んでくる。
 */
public record CheckoutRequest(
        @NotBlank String userId,
        @Valid @NotNull OrdererForm orderer) {
}
