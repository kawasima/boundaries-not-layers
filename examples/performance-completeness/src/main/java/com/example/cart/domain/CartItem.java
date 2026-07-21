package com.example.cart.domain;

import java.util.UUID;

public record CartItem(UUID productId, int quantity) {
}
