package com.example.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.example.cart.domain.EmptyCart;
import com.example.cart.domain.Individual;
import com.example.cart.domain.Order;
import com.example.cart.domain.OrderId;
import com.example.cart.domain.OrderLine;
import com.example.cart.domain.Orderer;
import com.example.cart.domain.PlaceOrder;
import com.example.cart.domain.PlaceOrderResult;
import com.example.cart.domain.UserId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.Ok;
import net.unit8.raoh.Path;
import net.unit8.raoh.Result;
import org.junit.jupiter.api.Test;

/**
 * 価格・割引・合計のドメイン計算を DB 抜きで確かめる純粋テスト。ロジックは Souther の {@code placeOrder}
 * 振る舞い（＝生成された {@link PlaceOrder}）に載っているので、Spring を起動せず {@code of().apply(...)}
 * で直接検証できる。値オブジェクトには public コンストラクタが無いので、境界と同じく {@code decoder()} で組む。
 */
class OrderPricingTest {

    private static <T> T unwrap(Result<T> result) {
        return ((Ok<T>) result).value();
    }

    private static OrderLine line(long unitPrice, int quantity) {
        return unwrap(OrderLine.decoder().decode(Map.of(
                "productId", UUID.randomUUID().toString(),
                "quantity", (long) quantity,
                "unitPrice", unitPrice), Path.ROOT));
    }

    private static Orderer orderer() {
        return unwrap(Individual.decoder().decode(Map.of(
                "email", "taro@example.com", "name", "山田太郎"), Path.ROOT));
    }

    private static OrderId orderId() {
        return unwrap(OrderId.decoder().decode(UUID.randomUUID().toString(), Path.ROOT));
    }

    private static UserId userId() {
        return unwrap(UserId.decoder().decode(UUID.randomUUID().toString(), Path.ROOT));
    }

    private static Order place(OrderLine... lines) {
        PlaceOrderResult result = PlaceOrder.of().apply(orderId(), userId(), orderer(), List.of(lines));
        return assertInstanceOf(Order.class, result);
    }

    @Test
    void 明細の小計は単価かける数量() {
        // 1200 * 8 = 9600
        assertEquals(9600, place(line(1200, 8)).charge().subtotal().value());
    }

    @Test
    void 小計5000以上で10パーセント引き() {
        // 1200 * 8 = 9600 → 10% = 960 引いて 8640
        Order order = place(line(1200, 8));
        assertEquals(9600, order.charge().subtotal().value());
        assertEquals(960, order.charge().discount().value());
        assertEquals(8640, order.charge().total().value());
    }

    @Test
    void 小計5000未満なら割引なし() {
        // 1200 * 3 = 3600 → 割引 0
        Order order = place(line(1200, 3));
        assertEquals(3600, order.charge().subtotal().value());
        assertEquals(0, order.charge().discount().value());
        assertEquals(3600, order.charge().total().value());
    }

    @Test
    void 空カートは注文にできない() {
        PlaceOrderResult result = PlaceOrder.of().apply(orderId(), userId(), orderer(), List.of());
        assertInstanceOf(EmptyCart.class, result);
    }
}
