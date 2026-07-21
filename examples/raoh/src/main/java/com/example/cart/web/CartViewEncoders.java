package com.example.cart.web;

import static net.unit8.raoh.encode.MapEncoders.object;
import static net.unit8.raoh.encode.MapEncoders.property;
import static net.unit8.raoh.encode.ObjectEncoders.int_;
import static net.unit8.raoh.encode.ObjectEncoders.uuid;

import com.example.cart.domain.CartItem;
import com.example.cart.domain.Page;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.unit8.raoh.encode.Encoder;
import org.jspecify.annotations.Nullable;

/**
 * HTTP 応答境界の encoder。ドメインの値を、そのまま JSON になる {@code Map} へ変換する。
 *
 * <p>ここでは {@code ObjectEncoders.uuid()}（UUID を文字列化）を使う——JSON の相手は文字列を望む。
 * 同じ値オブジェクトでも、DB 書き込み境界（{@link com.example.cart.infrastructure.JooqCartEncoders}）
 * とは目的が違うので encoder を分けている。境界ごとに encoder を持つ、が徹底されている。
 */
public final class CartViewEncoders {

    private CartViewEncoders() {}

    /** {@link CartItem} → {@code {"productId": "...", "quantity": n}}。 */
    public static final Encoder<CartItem, Map<String, @Nullable Object>> CART_ITEM_VIEW = object(
            property("productId", CartItem::productId, uuid().contramap(pid -> pid.value())),
            property("quantity", CartItem::quantity, int_().contramap(q -> q.value())));

    /** {@link Page} を、item を {@link #CART_ITEM_VIEW} でエンコードした応答 {@code Map} にする。 */
    public static Map<String, @Nullable Object> pageView(Page<CartItem> page) {
        List<Map<String, @Nullable Object>> items = page.items().stream()
                .map(CART_ITEM_VIEW::encode)
                .toList();
        var body = new LinkedHashMap<String, @Nullable Object>();
        body.put("total", page.total());
        body.put("page", page.page());
        body.put("size", page.size());
        body.put("items", items);
        return body;
    }
}
