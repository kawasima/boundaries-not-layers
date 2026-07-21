package com.example.cart.domain;

import net.unit8.raoh.Result;

/**
 * 商品参照の gateway。DB 行をデコードして {@link Product} を返す。
 * 該当が無ければ Err（{@code product_not_found}）を返す。
 */
public interface ProductRepository {
    Result<Product> load(ProductId productId);
}
