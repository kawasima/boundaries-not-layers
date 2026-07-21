package com.example.cart.infrastructure;

import java.util.List;

/**
 * ページング結果の入れ物。Souther は生成しないので、境界（インフラ）側の小さな値として持つ。
 */
public record Page<T>(long total, int page, int size, List<T> items) {

    public Page {
        items = List.copyOf(items);
    }
}
