package com.example.cart.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * チェックアウトのドメインサービス（性能+完全性版）。
 *
 * <p>この例は純粋性を犠牲にする側なので、ドメインサービスがリポジトリを直接呼ぶ（不純）。それでも
 * 責務配置の問題は残る: 値オブジェクトは用意したのに、明細の小計・合算・割引・合計といった業務ロジックが
 * このサービスに集まっている。{@link Order} も {@link OrderLine} も値の入れ物。
 *
 * <p>見積という亜種が増えると差が出る。金額計算の置き場がドメインに無いので、{@link #issueQuote} は
 * {@link #checkout} と同じ割引・合計の計算を**もう一度**書くことになる。亜種が増えるほどこのサービスに
 * メソッドと重複が積み上がる。raoh 版は共有の振る舞い（PriceCart / Charge）を再利用して終端だけ替える。
 */
@Service
public class CheckoutService {

    private static final long DISCOUNT_THRESHOLD = 5000;
    private static final int DISCOUNT_RATE = 10;
    private static final int QUOTE_VALIDITY_DAYS = 30;

    private final CartReadRepository cartReadRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public CheckoutService(CartReadRepository cartReadRepository,
                           ProductRepository productRepository,
                           OrderRepository orderRepository) {
        this.cartReadRepository = cartReadRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order checkout(UUID userId, Orderer orderer) {
        List<CartItem> items = cartReadRepository.findAllItems(userId);
        if (items.isEmpty()) {
            throw new CartEmptyException();
        }
        List<OrderLine> lines = priceLines(items);
        long subtotal = subtotalOf(lines);
        long discount = subtotal >= DISCOUNT_THRESHOLD ? subtotal * DISCOUNT_RATE / 100 : 0;  // 割引ルール（1）
        long total = subtotal - discount;

        Order order = new Order(UUID.randomUUID(), userId, orderer, lines,
                new Money(subtotal), new Money(discount), new Money(total));
        orderRepository.save(order);
        return order;
    }

    /** 見積発行。checkout の亜種としてメソッドを追加する。金額計算は checkout と重複する。 */
    @Transactional(readOnly = true)
    public Quotation issueQuote(UUID userId, Orderer orderer) {
        // 見積は法人限定。区分をフィールドで持つので、実行時に文字どおり値を見て判定するしかない。
        if (orderer.type() != OrdererType.CORPORATION) {
            throw new IndividualCannotQuoteException();
        }
        List<CartItem> items = cartReadRepository.findAllItems(userId);
        if (items.isEmpty()) {
            throw new CartEmptyException();
        }
        List<OrderLine> lines = priceLines(items);
        long subtotal = subtotalOf(lines);
        long discount = subtotal >= DISCOUNT_THRESHOLD ? subtotal * DISCOUNT_RATE / 100 : 0;  // 割引ルール（2）checkout と同じ
        long total = subtotal - discount;

        return new Quotation(UUID.randomUUID(), userId, orderer, lines,
                new Money(subtotal), new Money(discount), new Money(total),
                LocalDate.now().plusDays(QUOTE_VALIDITY_DAYS));
    }

    private List<OrderLine> priceLines(List<CartItem> items) {
        List<OrderLine> lines = new ArrayList<>();
        for (CartItem item : items) {
            if (!productRepository.isNowOnSale(item.productId())) {
                throw new SaleEndedException();
            }
            long unitPrice = productRepository.priceOf(item.productId());
            long lineSubtotal = unitPrice * item.quantity();
            lines.add(new OrderLine(item.productId(), item.quantity(),
                    new Money(unitPrice), new Money(lineSubtotal)));
        }
        return lines;
    }

    private static long subtotalOf(List<OrderLine> lines) {
        long subtotal = 0;
        for (OrderLine line : lines) {
            subtotal += line.subtotal().amount();
        }
        return subtotal;
    }
}
