package com.example.cart.domain;

import java.util.function.BiFunction;
import net.unit8.raoh.Result;
import org.springframework.stereotype.Component;

/**
 * 「チェックアウトして注文を作る」ドメインの振る舞い。
 *
 * <p>共有の {@link PriceCart}（価格付け）を再利用し、終端で注文を組んで保存するだけ。金額計算は
 * {@link Charge}、注文の組み立ては {@link Order#place} に載っているので、ここに残るのは合成の配線だけ。
 */
@Component
public class PlaceOrder implements BiFunction<UserId, Orderer, Result<Order>> {

    private final PriceCart priceCart;
    private final OrderRepository orders;
    private final DiscountPolicy discountPolicy = DiscountPolicy.standard();

    public PlaceOrder(PriceCart priceCart, OrderRepository orders) {
        this.priceCart = priceCart;
        this.orders = orders;
    }

    @Override
    public Result<Order> apply(UserId userId, Orderer orderer) {
        return priceCart.apply(userId)                                              // 共有: 価格付け
                .flatMap(lines -> Order.place(OrderId.newId(), userId, orderer, lines, discountPolicy))  // 終端: 注文を組む
                .map(order -> {                                                     // gateway: 保存
                    orders.save(order);
                    return order;
                });
    }
}
