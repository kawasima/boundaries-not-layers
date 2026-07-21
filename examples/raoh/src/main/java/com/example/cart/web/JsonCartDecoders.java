package com.example.cart.web;

import static net.unit8.raoh.json.JsonDecoders.*;

import com.example.cart.domain.Orderer;
import com.example.cart.domain.ProductId;
import com.example.cart.domain.Quantity;
import com.example.cart.domain.UserId;
import net.unit8.raoh.decode.Decoder;
import net.unit8.raoh.decode.combinator.Tuple2;
import net.unit8.raoh.decode.combinator.Tuple3;
import net.unit8.raoh.json.JsonDecoder;
import tools.jackson.databind.JsonNode;

/**
 * HTTP 境界（JSON）の decoder。リクエストボディ（{@link JsonNode}）を型付きの値へ変換する。
 *
 * <p>専用の Command 型は作らず、検証済みの3値を raoh の {@link Tuple3} にまとめて返す。
 * {@code combine} が userId / productId / quantity の検証エラーを蓄積するので、最初の失敗で
 * 止まらず全部まとめて返せる。各 {@code field} が JSON パス（{@code /userId} 等）を付ける。
 */
public final class JsonCartDecoders {

    private JsonCartDecoders() {}

    /**
     * {@code {"userId": "...uuid...", "productId": "...uuid...", "quantity": 8}} を
     * {@code Tuple3<UserId, ProductId, Quantity>} へデコードする。
     */
    public static final JsonDecoder<Tuple3<UserId, ProductId, Quantity>> ADD_ITEM = wrap(
            combine(
                    // string().uuid() は UUID 形式を検証して UUID を返す。map で値オブジェクトに包む。
                    field("userId", string().uuid().map(UserId::new)),
                    field("productId", string().uuid().map(ProductId::new)),
                    // int_().positive() が「1以上」を保証する。振る舞い側はもうこの検証を書かない。
                    field("quantity", int_().positive().map(Quantity::new))
            ).map(Tuple3::new));

    /**
     * チェックアウト要求を {@code Tuple2<UserId, Orderer>} へデコードする。
     * <pre>{@code {"userId":"...uuid...","orderer":{"type":"individual","email":"...","name":"..."}}}</pre>
     * 注文者は {@link JsonOrdererDecoders#ORDERER} が個人／法人に判別して decode する。
     */
    public static final JsonDecoder<Tuple2<UserId, Orderer>> CHECKOUT = wrap(
            combine(
                    field("userId", string().uuid().map(UserId::new)),
                    field("orderer", JsonOrdererDecoders.ORDERER)
            ).map(Tuple2::new));

    /** {@code Decoder<JsonNode, T>} を {@link JsonDecoder} として見る（関数型IFなのでメソッド参照で足りる）。 */
    private static <T> JsonDecoder<T> wrap(Decoder<JsonNode, T> dec) {
        return dec::decode;
    }
}
