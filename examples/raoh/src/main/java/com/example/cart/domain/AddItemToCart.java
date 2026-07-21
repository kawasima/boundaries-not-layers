package com.example.cart.domain;

import net.unit8.raoh.Result;
import net.unit8.raoh.decode.combinator.Function3;
import org.springframework.stereotype.Component;

/**
 * 「カートに商品を入れる」ドメインの振る舞い。単一責務の振る舞い（{@link Product#ensureOnSale} /
 * {@code Cart.add}）を gateway と {@code flatMap} で合成しただけの Function。
 *
 * <p>引数はドメインの値そのもの（{@link UserId} / {@link ProductId} / {@link Quantity}）。境界の
 * decoder が出す {@code Tuple3} は Controller で分解し、ここへは意味のある型で渡す。出力は成否を値で
 * 表す {@link Result}。どこかで Err になれば以降は短絡し、その Err がそのまま返る。
 *
 * <p>依存するのは domain のポート（{@link ProductRepository} / {@link CartRepository}）だけ。
 * トランザクション境界はここには持たない——それは操作を起動する境界（Controller）の関心。
 */
@Component
public class AddItemToCart implements Function3<UserId, ProductId, Quantity, Result<CartItem>> {

    private final ProductRepository products;
    private final CartRepository carts;

    public AddItemToCart(ProductRepository products, CartRepository carts) {
        this.products = products;
        this.carts = carts;
    }

    @Override
    public Result<CartItem> apply(UserId userId, ProductId productId, Quantity quantity) {
        return products.load(productId)                       // gateway: 商品を読む
                .flatMap(Product::ensureOnSale)               // 振る舞い: 販売中か
                .flatMap(product -> carts.loadForUpdate(userId)  // gateway: カートを読む
                        .flatMap(cart -> cart.add(productId, quantity)  // 振る舞い: 上限内で追加
                                .map(item -> {                // gateway: Ok のときだけ書き込む
                                    carts.addItem(cart.id(), item);
                                    return item;
                                })));
    }
}
