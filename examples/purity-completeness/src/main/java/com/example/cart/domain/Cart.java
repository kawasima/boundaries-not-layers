package com.example.cart.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 純粋性+完全性を両立させた Cart。
 *
 * <p>不変条件（合計数量が上限を超えない）を、ドメイン自身が判断できる。そのために
 * カートに入っている全アイテムをメモリ上に保持する。外界（DB）とはやり取りせず、
 * 手元の情報だけで判断が完結するので「純粋」かつ「完全」。
 *
 * <p>その代償として、更新のたびに全アイテムをロードする必要があり、アイテム数が
 * 増えるほど性能が悪化する。ここで犠牲になっているのは「性能」。
 */
public class Cart {

    /** カートに入れられる合計数量の上限。 */
    private static final int UPPER_BOUND = 100;

    private final List<CartItem> items;

    public Cart(List<CartItem> items) {
        this.items = new ArrayList<>(items);
        if (!isValid()) {
            throw new CartFullException();
        }
    }

    public Cart() {
        this(List.of());
    }

    private boolean isValid() {
        int total = items.stream().mapToInt(CartItem::quantity).sum();
        return total <= UPPER_BOUND;
    }

    /**
     * 商品を追加する。既に入っていれば数量を合算する。
     * 追加の結果、合計が上限を超えたら {@link CartFullException} を投げる。
     */
    public void add(UUID productId, int quantity) {
        int index = indexOf(productId);
        if (index < 0) {
            items.add(new CartItem(productId, quantity));
        } else {
            CartItem current = items.get(index);
            items.set(index, new CartItem(productId, current.quantity() + quantity));
        }
        if (!isValid()) {
            throw new CartFullException();
        }
    }

    private int indexOf(UUID productId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productId().equals(productId)) {
                return i;
            }
        }
        return -1;
    }

    public List<CartItem> items() {
        return List.copyOf(items);
    }
}
