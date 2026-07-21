package com.example.cart.domain;

import java.util.UUID;

/**
 * 注文明細。単価も小計もフィールドに持つが、小計を「単価×数量」で求めるのは明細ではなく
 * {@link CheckoutService} の仕事になっている。ここは値の入れ物。
 */
public record OrderLine(UUID productId, int quantity, Money unitPrice, Money subtotal) {
}
