package com.example.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.example.cart.domain.DiscountPolicy;
import com.example.cart.domain.Email;
import com.example.cart.domain.Money;
import com.example.cart.domain.Orderer;
import com.example.cart.domain.Order;
import com.example.cart.domain.OrderId;
import com.example.cart.domain.OrderLine;
import com.example.cart.domain.Product;
import com.example.cart.domain.ProductId;
import com.example.cart.domain.Quantity;
import com.example.cart.domain.UserId;
import java.util.List;
import java.util.UUID;
import net.unit8.raoh.Ok;
import net.unit8.raoh.Result;
import org.junit.jupiter.api.Test;

/**
 * 価格・割引・合計のドメイン計算を DB 抜きで確かめる純粋なテスト。ロジックが Product / OrderLine /
 * Order / DiscountPolicy に載っているので、こうして単体で正確に検証できる。
 */
class OrderPricingTest {

    private static Product product(long price) {
        return new Product(new ProductId(UUID.randomUUID()), true, new Money(price));
    }

    private static Orderer orderer() {
        return new Orderer.Individual(new Email("taro@example.com"), "山田太郎");
    }

    @Test
    void 明細の小計は単価かける数量() {
        OrderLine line = OrderLine.of(product(1200), new Quantity(8));
        assertEquals(9600, line.subtotal().amount());
    }

    @Test
    void 小計5000以上で10パーセント引き() {
        // 1200 * 8 = 9600 → 10% = 960 引いて 8640
        OrderLine line = OrderLine.of(product(1200), new Quantity(8));
        Result<Order> result = Order.place(
                OrderId.newId(), new UserId(UUID.randomUUID()), orderer(), List.of(line), DiscountPolicy.standard());

        assertInstanceOf(Ok.class, result);
        Order order = ((Ok<Order>) result).value();
        assertEquals(9600, order.subtotal().amount());
        assertEquals(960, order.discount().amount());
        assertEquals(8640, order.total().amount());
    }

    @Test
    void 小計5000未満なら割引なし() {
        // 1200 * 3 = 3600 → 割引 0
        OrderLine line = OrderLine.of(product(1200), new Quantity(3));
        Order order = ((Ok<Order>) Order.place(
                OrderId.newId(), new UserId(UUID.randomUUID()), orderer(), List.of(line), DiscountPolicy.standard())).value();
        assertEquals(3600, order.subtotal().amount());
        assertEquals(0, order.discount().amount());
        assertEquals(3600, order.total().amount());
    }

    @Test
    void 空カートは注文にできない() {
        Result<Order> result = Order.place(
                OrderId.newId(), new UserId(UUID.randomUUID()), orderer(), List.of(), DiscountPolicy.standard());
        assertEquals("empty_cart", ((net.unit8.raoh.Err<Order>) result).issues().asList().get(0).code());
    }
}
