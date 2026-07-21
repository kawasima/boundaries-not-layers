package com.example.cart.domain;

import net.unit8.raoh.Path;
import net.unit8.raoh.Result;

/**
 * 商品。DB 行を decode して得る、販売状態と単価まで持つドメインオブジェクト。
 *
 * <p>{@link #ensureOnSale} は「販売中か」という単一責務の純粋な振る舞い。価格は Product が知っている——
 * だから明細の価格付け（{@link OrderLine#of}）は Product に単価を尋ねるだけで済み、価格計算ロジックを
 * サービスに集める必要がない。
 */
public record Product(ProductId id, boolean onSale, Money price) {

    /** 販売中なら自分自身を Ok で、そうでなければ Err を返す。 */
    public Result<Product> ensureOnSale() {
        return onSale
                ? Result.ok(this)
                : Result.fail(Path.ROOT, "sale_ended", "販売が終了しました");
    }
}
