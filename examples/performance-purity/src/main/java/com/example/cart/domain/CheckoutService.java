package com.example.cart.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * チェックアウトのドメインサービス（純粋）。
 *
 * <p>純粋性は守れている——が、値オブジェクトを用意したのに、明細の小計・合算・割引・合計といった業務
 * ロジックがこのサービスに集まっている。{@link Order} も {@link OrderLine} も値の入れ物。
 *
 * <p>そして見積という亜種が増えると、その差がはっきり出る。金額計算はドメインオブジェクトに置き場が
 * 無いので、{@link #issueQuote} は {@link #checkout} と同じ割引・合計の計算を**もう一度**書くことになる
 * （明細のループは private ヘルパに切り出せても、割引ルールは各メソッドに重複する）。亜種が増えるほど
 * このサービスにメソッドと重複が積み上がる。raoh 版は共有の振る舞い（PriceCart / Charge）を再利用して
 * 終端だけ差し替えるので、注文の処理には手を入れない。
 */
@Service
public class CheckoutService {

    private static final long DISCOUNT_THRESHOLD = 5000;
    private static final int DISCOUNT_RATE = 10;
    private static final int QUOTE_VALIDITY_DAYS = 30;

    public Order checkout(UUID userId, Orderer orderer, List<CartItem> items, Map<UUID, ProductSnapshot> products) {
        if (items.isEmpty()) {
            throw new CartEmptyException();
        }
        List<OrderLine> lines = priceLines(items, products);
        long subtotal = subtotalOf(lines);
        long discount = subtotal >= DISCOUNT_THRESHOLD ? subtotal * DISCOUNT_RATE / 100 : 0;  // 割引ルール（1）
        long total = subtotal - discount;

        return new Order(UUID.randomUUID(), userId, orderer, lines,
                new Money(subtotal), new Money(discount), new Money(total));
    }

    /** 見積発行。checkout の亜種としてメソッドを追加する。金額計算は checkout と重複する。 */
    public Quotation issueQuote(UUID userId, Orderer orderer, List<CartItem> items,
                                Map<UUID, ProductSnapshot> products, LocalDate today) {
        // 見積は法人限定。区分をフィールドで持つので、実行時に文字どおり値を見て判定するしかない。
        if (orderer.type() != OrdererType.CORPORATION) {
            throw new IndividualCannotQuoteException();
        }
        if (items.isEmpty()) {
            throw new CartEmptyException();
        }
        List<OrderLine> lines = priceLines(items, products);
        long subtotal = subtotalOf(lines);
        long discount = subtotal >= DISCOUNT_THRESHOLD ? subtotal * DISCOUNT_RATE / 100 : 0;  // 割引ルール（2）checkout と同じ
        long total = subtotal - discount;

        return new Quotation(UUID.randomUUID(), userId, orderer, lines,
                new Money(subtotal), new Money(discount), new Money(total),
                today.plusDays(QUOTE_VALIDITY_DAYS));
    }

    private static List<OrderLine> priceLines(List<CartItem> items, Map<UUID, ProductSnapshot> products) {
        List<OrderLine> lines = new ArrayList<>();
        for (CartItem item : items) {
            ProductSnapshot product = products.get(item.productId());
            if (product == null || !product.onSale()) {
                throw new SaleEndedException();
            }
            long unitPrice = product.price();
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
