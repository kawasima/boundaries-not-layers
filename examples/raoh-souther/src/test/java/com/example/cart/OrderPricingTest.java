package com.example.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.example.cart.domain.EmptyCart;
import com.example.cart.domain.Order;
import com.example.cart.domain.OrderPlaced;
import com.example.cart.domain.PlaceOrder;
import com.example.cart.domain.PlaceOrderCommand;
import com.example.cart.domain.PlaceOrderResult;
import com.example.cart.domain.PriceCart;
import com.example.cart.domain.PriceCartResult;
import com.example.cart.domain.PricedCart;
import com.example.cart.domain.SaveOrder;
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
 * 振る舞いに載っている。placeOrder は priceCart と saveOrder を INJECTED で要求するので、in-memory の
 * フェイクを {@link PlaceOrder#bind} して直接検証する。値には public コンストラクタが無いので、境界と同じく
 * {@code decoder()} で組む。
 */
class OrderPricingTest {

    private static <T> T unwrap(Result<T> result) {
        return ((Ok<T>) result).value();
    }

    /** 単価 unit・数量 qty の明細1行だけを返す priceCart フェイク。 */
    private static PriceCart fakePrice(long unit, int qty) {
        return new PriceCart() {
            @Override
            public PriceCartResult apply(UserId user) {
                Map<String, Object> line = Map.of(
                        "productId", UUID.randomUUID().toString(),
                        "quantity", (long) qty,
                        "unitPrice", unit);
                return unwrap(PricedCart.decoder().decode(Map.of("lines", List.of(line)), Path.ROOT));
            }
        };
    }

    /** 空明細を返す priceCart フェイク（空カート）。 */
    private static PriceCart emptyPrice() {
        return new PriceCart() {
            @Override
            public PriceCartResult apply(UserId user) {
                return unwrap(PricedCart.decoder().decode(Map.of("lines", List.of()), Path.ROOT));
            }
        };
    }

    /** 受け取った Order をそのまま OrderPlaced に包む saveOrder フェイク。 */
    private static final SaveOrder FAKE_SAVE = new SaveOrder() {
        @Override
        public OrderPlaced apply(Order order) {
            return unwrap(OrderPlaced.decoder().decode(
                    Map.of("order", Order.encoder().encode(order)), Path.ROOT));
        }
    };

    private static PlaceOrderCommand command() {
        return unwrap(PlaceOrderCommand.decoder().decode(Map.of(
                "orderId", UUID.randomUUID().toString(),
                "userId", UUID.randomUUID().toString(),
                "orderer", Map.of("type", "Individual", "email", "taro@example.com", "name", "山田太郎")),
                Path.ROOT));
    }

    private static Order place(long unit, int qty) {
        PlaceOrderResult result = PlaceOrder.bind(fakePrice(unit, qty), FAKE_SAVE).apply(command());
        return ((OrderPlaced) assertInstanceOf(OrderPlaced.class, result)).order();
    }

    @Test
    void 明細の小計は単価かける数量() {
        // 1200 * 8 = 9600
        assertEquals(9600, place(1200, 8).charge().subtotal().value());
    }

    @Test
    void 小計5000以上で10パーセント引き() {
        // 1200 * 8 = 9600 → 10% = 960 引いて 8640
        Order order = place(1200, 8);
        assertEquals(9600, order.charge().subtotal().value());
        assertEquals(960, order.charge().discount().value());
        assertEquals(8640, order.charge().total().value());
    }

    @Test
    void 小計5000未満なら割引なし() {
        // 1200 * 3 = 3600 → 割引 0
        Order order = place(1200, 3);
        assertEquals(3600, order.charge().subtotal().value());
        assertEquals(0, order.charge().discount().value());
        assertEquals(3600, order.charge().total().value());
    }

    @Test
    void 空カートは注文にできない() {
        PlaceOrderResult result = PlaceOrder.bind(emptyPrice(), FAKE_SAVE).apply(command());
        assertInstanceOf(EmptyCart.class, result);
    }
}
