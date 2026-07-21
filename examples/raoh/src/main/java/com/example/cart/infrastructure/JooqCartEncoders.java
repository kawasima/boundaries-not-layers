package com.example.cart.infrastructure;

import static net.unit8.raoh.encode.MapEncoders.object;
import static net.unit8.raoh.encode.MapEncoders.property;
import static net.unit8.raoh.encode.ObjectEncoders.int_;

import com.example.cart.domain.CartItem;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.encode.Encoder;
import org.jspecify.annotations.Nullable;

/**
 * DB 書き込み境界の encoder——{@link JooqCartDecoders} の書き込み側の鏡。
 *
 * <p>{@link JooqCartDecoders#CART_ITEM} が行を {@link CartItem} にデコードするのに対し、
 * こちらは {@link CartItem} を列の {@code Map} に戻す。{@code contramap} が値オブジェクトを
 * 開く（{@code ProductId -> UUID}、{@code Quantity -> int}）。cart_id と cart_item_id は
 * リポジトリが補うので、この形には含めない（DB 採番に相当）。
 */
public final class JooqCartEncoders {

    private JooqCartEncoders() {}

    /**
     * {@link CartItem} を INSERT/UPDATE が扱う列 {@code (product_id, quantity)} にエンコードする。
     *
     * <p>{@code ObjectEncoders.uuid()} は UUID を文字列化するが、ここでは jOOQ に UUID 型のまま
     * 渡したいので、素通しの UUID エンコーダ（{@code v -> v}）を使う。
     */
    public static final Encoder<CartItem, Map<String, @Nullable Object>> CART_ITEM_ROW = object(
            property("product_id", ci -> ci.productId().value(), rawUuid()),
            property("quantity", ci -> ci.quantity().value(), int_()));

    /** UUID を変換せずそのまま渡すエンコーダ（jOOQ/H2 の UUID 列にバインドするため）。 */
    private static Encoder<UUID, Object> rawUuid() {
        return v -> v;
    }
}
