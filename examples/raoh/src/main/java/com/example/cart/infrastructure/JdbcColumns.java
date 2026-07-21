package com.example.cart.infrastructure;

import java.util.UUID;
import net.unit8.raoh.ErrorCodes;
import net.unit8.raoh.Result;
import net.unit8.raoh.decode.Decoder;
import org.jspecify.annotations.Nullable;

/**
 * jOOQ の生値用の小さな decoder 部品。{@code ObjectDecoders} に無いものだけをここに置く。
 */
final class JdbcColumns {

    private JdbcColumns() {}

    /**
     * jOOQ の生値（{@code java.util.UUID}）を UUID にデコードする。{@code ObjectDecoders} には
     * UUID 用が無い——境界に必要な部品は、この通り数行で自前で作れる。文字列で来た場合も受ける。
     */
    static Decoder<@Nullable Object, UUID> uuid() {
        return (in, path) -> switch (in) {
            case null -> Result.fail(path, ErrorCodes.REQUIRED, "is required");
            case UUID u -> Result.ok(u);
            case String s -> {
                try {
                    yield Result.ok(UUID.fromString(s));
                } catch (IllegalArgumentException e) {
                    yield Result.fail(path, ErrorCodes.INVALID_FORMAT, "not a valid UUID");
                }
            }
            default -> Result.fail(path, ErrorCodes.TYPE_MISMATCH, "expected uuid");
        };
    }
}
