package com.example.cart.web;

import com.example.cart.domain.CartItem;
import com.example.cart.infrastructure.Page;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 応答境界の encoder。Souther の {@link CartItem} を public アクセサで開き、そのまま JSON になる
 * {@code Map} に載せる。productId は UUID 文字列（Souther の String newtype）。
 */
public final class CartViewEncoders {

    private CartViewEncoders() {}

    public static Map<String, Object> itemView(CartItem item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("productId", item.productId().value());
        m.put("quantity", item.quantity().value());
        return m;
    }

    public static Map<String, Object> pageView(Page<CartItem> page) {
        List<Map<String, Object>> items = page.items().stream()
                .map(CartViewEncoders::itemView)
                .toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", page.total());
        body.put("page", page.page());
        body.put("size", page.size());
        body.put("items", items);
        return body;
    }
}
