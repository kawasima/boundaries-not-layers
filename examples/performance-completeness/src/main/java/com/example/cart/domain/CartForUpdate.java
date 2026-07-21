package com.example.cart.domain;

import java.util.UUID;

/**
 * 性能+完全性を両立させた「更新用」の Cart。純粋性を犠牲にしている。
 *
 * <p>不変条件（合計数量の上限）のチェックをドメイン自身に持たせたい。しかし、
 * そのために全アイテムをインスタンス化して持つのは性能上避けたい。そこで、
 * 更新に必要な合計数量だけをリポジトリに問い合わせて確認する。
 *
 * <p>結果として、ドメインオブジェクトが更新の最中に外界（リポジトリ）とやり取りする。
 * 完全（不変条件をドメインが判断する）かつ高速（全ロードしない）だが、純粋ではない。
 */
public class CartForUpdate {

    private static final int UPPER_BOUND = 10000;

    private final UUID cartId;

    public CartForUpdate(UUID cartId) {
        this.cartId = cartId;
    }

    /**
     * 商品を追加する。上限チェックのための合計数量取得と、追加そのものを、
     * ドメインの内側からリポジトリへ委譲する（＝純粋性を犠牲にしている箇所）。
     */
    public void add(UUID productId, int quantity, CartWriteRepository cartRepository) {
        int total = cartRepository.getItemCount(cartId);
        if (total + quantity > UPPER_BOUND) {
            throw new CartFullException();
        }
        cartRepository.addItem(cartId, new CartItem(productId, quantity));
    }
}
