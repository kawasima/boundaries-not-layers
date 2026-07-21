package com.example.cart.domain;

import java.util.UUID;

/** カートの識別子。DB の行からデコードして得る。 */
public record CartId(UUID value) {
}
