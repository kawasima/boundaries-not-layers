package com.example.cart.domain;

/**
 * 数量。1以上であることは境界の decoder（{@code int_().positive()}）が保証する。
 * ここに到達した時点で正の値であることが型で約束されている。
 */
public record Quantity(int value) {
}
