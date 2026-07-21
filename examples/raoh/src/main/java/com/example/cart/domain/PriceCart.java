package com.example.cart.domain;

import java.util.List;
import java.util.function.Function;
import net.unit8.raoh.Result;
import org.springframework.stereotype.Component;

/**
 * カートを価格付きの明細に変換する、共有の振る舞い。
 *
 * <p>「全明細を読む → 各商品を読む → 販売中か → 明細に価格付け」という価格計算の中核。注文（{@link PlaceOrder}）
 * でも見積（{@link IssueQuote}）でも同じものを使う。亜種が増えても、この振る舞いを再利用して終端だけ
 * 差し替えればよい。これが合成の効くところ。
 */
@Component
public class PriceCart implements Function<UserId, Result<List<OrderLine>>> {

    private final CartQueryRepository carts;
    private final ProductRepository products;

    public PriceCart(CartQueryRepository carts, ProductRepository products) {
        this.carts = carts;
        this.products = products;
    }

    @Override
    public Result<List<OrderLine>> apply(UserId userId) {
        List<CartItem> items = carts.findAllItems(userId);
        return Result.traverseResults(items, item ->
                products.load(item.productId())
                        .flatMap(Product::ensureOnSale)
                        .map(product -> OrderLine.of(product, item.quantity())));
    }
}
