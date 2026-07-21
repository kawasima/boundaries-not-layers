package com.example.cart.web;

import static net.unit8.raoh.json.JsonDecoders.combine;
import static net.unit8.raoh.json.JsonDecoders.field;
import static net.unit8.raoh.json.JsonDecoders.int_;
import static net.unit8.raoh.json.JsonDecoders.string;

import com.example.cart.domain.Orderer;
import com.example.cart.domain.ProductId;
import com.example.cart.domain.Quantity;
import com.example.cart.domain.UserId;
import java.util.UUID;
import net.unit8.raoh.Path;
import net.unit8.raoh.decode.Decoder;
import net.unit8.raoh.decode.combinator.Tuple2;
import net.unit8.raoh.decode.combinator.Tuple3;
import tools.jackson.databind.JsonNode;

/**
 * HTTP 境界（JSON）の decoder。各フィールドを raoh で検証（UUID 形式・正の数量）してから、
 * その正規化済みスカラを Souther 生成型の {@code decoder()} に渡してドメイン値を組む。UUID・数量の
 * 形式エラーはここ（decode）で出るので 400、業務ルール違反は振る舞いの出力なので 422、と2段に分かれる。
 */
public final class JsonCartDecoders {

    private JsonCartDecoders() {}

    public static final Decoder<JsonNode, Tuple3<UserId, ProductId, Quantity>> ADD_ITEM = combine(
            field("userId", string().uuid().map(UUID::toString)
                    .flatMap(s -> UserId.decoder().decode(s, Path.ROOT))),
            field("productId", string().uuid().map(UUID::toString)
                    .flatMap(s -> ProductId.decoder().decode(s, Path.ROOT))),
            field("quantity", int_().positive()
                    .flatMap(n -> Quantity.decoder().decode(n.longValue(), Path.ROOT)))
    ).map(Tuple3::new);

    public static final Decoder<JsonNode, Tuple2<UserId, Orderer>> CHECKOUT = combine(
            field("userId", string().uuid().map(UUID::toString)
                    .flatMap(s -> UserId.decoder().decode(s, Path.ROOT))),
            field("orderer", JsonOrdererDecoders.ORDERER)
    ).map(Tuple2::new);
}
