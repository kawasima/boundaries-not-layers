package com.example.cart.web;

import static net.unit8.raoh.json.JsonDecoders.combine;
import static net.unit8.raoh.json.JsonDecoders.discriminate;
import static net.unit8.raoh.json.JsonDecoders.field;
import static net.unit8.raoh.json.JsonDecoders.string;
import static net.unit8.raoh.json.JsonDecoders.variant;

import com.example.cart.domain.Email;
import com.example.cart.domain.Orderer;
import java.util.regex.Pattern;
import net.unit8.raoh.decode.Decoder;
import tools.jackson.databind.JsonNode;

/**
 * 注文者（{@link Orderer}）の JSON 判別 decoder。
 *
 * <p>{@code type} フィールドで個人／法人を判別し、それぞれ専用の decoder に振り分ける。個人は氏名、
 * 法人は会社名と法人番号（13桁）を検証する。decode に成功した時点で、その注文者は個人なら個人、法人なら
 * 法人として完全に型付いている。「法人番号の無い法人」は decode を通らないので、下流に来ない。
 */
public final class JsonOrdererDecoders {

    private JsonOrdererDecoders() {}

    /** 個人: {@code {"type":"individual","email":"...","name":"..."}} */
    static final Decoder<JsonNode, Orderer.Individual> INDIVIDUAL = combine(
            field("email", string().trim().toLowerCase().email().map(Email::new)),
            field("name", string().trim().nonBlank().maxLength(100))
    ).map(Orderer.Individual::new);

    /** 法人: {@code {"type":"corporation","email":"...","companyName":"...","corporateNumber":"1234567890123"}} */
    static final Decoder<JsonNode, Orderer.Corporation> CORPORATION = combine(
            field("email", string().trim().toLowerCase().email().map(Email::new)),
            field("companyName", string().trim().nonBlank().maxLength(200)),
            field("corporateNumber", string().pattern(Pattern.compile("\\d{13}")))
    ).map(Orderer.Corporation::new);

    public static final Decoder<JsonNode, Orderer> ORDERER = discriminate("type",
            variant("individual", INDIVIDUAL),
            variant("corporation", CORPORATION));
}
