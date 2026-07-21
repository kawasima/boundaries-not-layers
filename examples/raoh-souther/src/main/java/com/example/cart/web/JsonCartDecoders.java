package com.example.cart.web;

import static net.unit8.raoh.json.JsonDecoders.combine;
import static net.unit8.raoh.json.JsonDecoders.field;
import static net.unit8.raoh.json.JsonDecoders.int_;
import static net.unit8.raoh.json.JsonDecoders.string;

import com.example.cart.domain.AddItemCommand;
import com.example.cart.domain.Orderer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.Path;
import net.unit8.raoh.decode.Decoder;
import net.unit8.raoh.decode.combinator.Tuple2;
import tools.jackson.databind.JsonNode;

/**
 * HTTP 境界（JSON）の decoder。各フィールドを raoh で検証（UUID 形式・正の数量）してから、その正規化済み
 * スカラを Souther のコマンド record の {@code decoder()}（フィールド名キーの Map を取る）に渡してコマンドを
 * 組む。UUID・数量の形式エラーはここ（decode）で出るので 400、業務ルール違反は振る舞いの出力なので 422。
 *
 * <p>{@link #ADD_ITEM} はコマンド record を直接組む。{@link #CHECKOUT} は userId を String のまま残し
 * Orderer と組にして返す（注文/見積の ID や validUntil は Controller が採番してコマンドを組み立てる）。
 */
public final class JsonCartDecoders {

    private JsonCartDecoders() {}

    /** {@code {"userId":"...","productId":"...","quantity":n}} を {@link AddItemCommand} にする。 */
    public static final Decoder<JsonNode, AddItemCommand> ADD_ITEM = combine(
            field("userId", string().uuid().map(UUID::toString)),
            field("productId", string().uuid().map(UUID::toString)),
            field("quantity", int_().positive())
    ).map((userId, productId, quantity) -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", userId);
        m.put("productId", productId);
        m.put("quantity", quantity.longValue());
        return m;
    }).flatMap(m -> AddItemCommand.decoder().decode(m, Path.ROOT));

    /** {@code {"userId":"...","orderer":{...}}} を {@code (userId, Orderer)} にする。userId は String のまま。 */
    public static final Decoder<JsonNode, Tuple2<String, Orderer>> CHECKOUT = combine(
            field("userId", string().uuid().map(UUID::toString)),
            field("orderer", JsonOrdererDecoders.ORDERER)
    ).map(Tuple2::new);
}
