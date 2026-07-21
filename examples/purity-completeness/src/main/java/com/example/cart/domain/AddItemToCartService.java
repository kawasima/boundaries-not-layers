package com.example.cart.domain;

import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * カートに商品を入れる業務判断を持つドメインサービス（純粋）。
 *
 * <p><b>なぜドメインサービスなのか（藁人形ではない理由）:</b> 「商品が販売中であること（Product 側の
 * 状態）」と「カートの合計数量が上限を超えないこと（Cart の不変条件）」の両方を満たすかは、二つの
 * 集約にまたがる判断で、どちらの集約にも属せない。Cart に持たせると Cart が販売状態を知ってしまう。
 * だからこの調整はドメインサービスに置く。
 *
 * <p><b>純粋性:</b> この例は純粋性を守る（性能を犠牲にする）側なので、ドメインはリポジトリを呼ばない。
 * ロード済みの {@link Cart} と、商品の販売状態（{@code boolean}）を受け取るだけ。DB 読み書きはアプリ層
 * （use case）の仕事。
 *
 * <p><b>その代償:</b> 入力（{@code UUID} / {@code boolean} / {@code int}）は不変条件を型では保証しない。
 * {@code int quantity} は 0 や負も通す。ドメインサービスは複数文脈から再利用され得る能力なので、全
 * 呼び出し元が検証済みだとは仮定できず、自分の不変条件を自分でも守る羽目になる（下の {@code quantity
 * <= 0}）。呼び出し側（use case）の {@code requirePositive} と同じ検査が二重になる。raoh 版は decoder が
 * {@code Quantity} 型を構築した時点で正を保証するので、この再検査は無い。
 */
@Service
public class AddItemToCartService {

    public void addItem(Cart cart, UUID productId, boolean productOnSale, int quantity) {
        // 入力の型（int）が「正であること」を保証しないので、ここでも確かめざるを得ない。
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        // 二つの集約にまたがる判断: 販売中（Product の状態）かつ 上限内（Cart の不変条件）。
        if (!productOnSale) {
            throw new SaleEndedException();
        }
        cart.add(productId, quantity);  // 上限の不変条件は Cart 自身が守る。ここも I/O はしない。
    }
}
