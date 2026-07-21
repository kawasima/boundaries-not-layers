package com.example.cart.domain;

/**
 * 注文明細。商品・数量・そのとき確定した単価を持つ。
 *
 * <p>価格付けは明細自身の責務。{@link #of} は Product に単価を尋ねて明細を作り、{@link #subtotal}
 * は単価×数量を返す。この計算はここに閉じ、チェックアウトのサービスへは漏れない。
 */
public record OrderLine(ProductId productId, Quantity quantity, Money unitPrice) {

    /** 商品と数量から明細を作る。単価は Product が持つものを使う。 */
    public static OrderLine of(Product product, Quantity quantity) {
        return new OrderLine(product.id(), quantity, product.price());
    }

    /** この明細の小計（単価×数量）。 */
    public Money subtotal() {
        return unitPrice.times(quantity.value());
    }
}
