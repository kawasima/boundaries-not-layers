package com.example.cart.web;

import static net.unit8.raoh.json.JsonDecoders.combine;
import static net.unit8.raoh.json.JsonDecoders.discriminate;
import static net.unit8.raoh.json.JsonDecoders.field;
import static net.unit8.raoh.json.JsonDecoders.string;
import static net.unit8.raoh.json.JsonDecoders.variant;

import com.example.cart.domain.Corporation;
import com.example.cart.domain.Individual;
import com.example.cart.domain.Orderer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.unit8.raoh.Path;
import net.unit8.raoh.decode.Decoder;
import tools.jackson.databind.JsonNode;

/**
 * 注文者（{@link Orderer}）の JSON 判別 decoder。ここが raoh-souther の境界（JSON→ドメイン）の継ぎ目。
 *
 * <p>{@code type} で個人／法人を判別し、各フィールドを raoh で正規化・検証する（email は trim/小文字化して
 * 形式検査、法人番号は 13 桁の正規表現——Souther には正規表現が無いので、この 13 桁ゲートは raoh の責務）。
 * 検証済みのフィールドを Souther 生成型の {@code decoder()}（フィールド名キーの Map を取る）に渡して
 * {@link Individual} / {@link Corporation} を組む。Email の不変条件（"@" を含む）はここで再検査される。
 */
public final class JsonOrdererDecoders {

    private JsonOrdererDecoders() {}

    /** 個人: {@code {"type":"individual","email":"...","name":"..."}} */
    static final Decoder<JsonNode, Individual> INDIVIDUAL = combine(
            field("email", string().trim().toLowerCase().email()),
            field("name", string().trim().nonBlank().maxLength(100))
    ).map((email, name) -> map("email", email, "name", name))
            .flatMap(m -> Individual.decoder().decode(m, Path.ROOT));

    /** 法人: {@code {"type":"corporation","email":"...","companyName":"...","corporateNumber":"1234567890123"}} */
    static final Decoder<JsonNode, Corporation> CORPORATION = combine(
            field("email", string().trim().toLowerCase().email()),
            field("companyName", string().trim().nonBlank().maxLength(200)),
            field("corporateNumber", string().pattern(Pattern.compile("\\d{13}")))
    ).map((email, companyName, corporateNumber) ->
                    map("email", email, "companyName", companyName, "corporateNumber", corporateNumber))
            .flatMap(m -> Corporation.decoder().decode(m, Path.ROOT));

    public static final Decoder<JsonNode, Orderer> ORDERER = discriminate("type",
            variant("individual", INDIVIDUAL),
            variant("corporation", CORPORATION));

    private static Map<String, Object> map(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private static Map<String, Object> map(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        Map<String, Object> m = map(k1, v1, k2, v2);
        m.put(k3, v3);
        return m;
    }
}
