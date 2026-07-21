package com.example.cart.domain;

/** メールアドレス。形式の検証は境界の decoder が行い、通ったものだけがこの型になる。 */
public record Email(String value) {
}
