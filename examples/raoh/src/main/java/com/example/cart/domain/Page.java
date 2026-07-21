package com.example.cart.domain;

import java.util.List;

/** 参照用のページング結果。 */
public record Page<T>(long total, int page, int size, List<T> items) {
}
