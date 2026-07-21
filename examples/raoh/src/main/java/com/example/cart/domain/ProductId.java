package com.example.cart.domain;

import java.util.UUID;

/** 商品の識別子。検証は境界の decoder が持つ。 */
public record ProductId(UUID value) {
}
