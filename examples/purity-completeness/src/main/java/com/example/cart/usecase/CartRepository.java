package com.example.cart.usecase;

import com.example.cart.domain.Cart;
import java.util.UUID;

/**
 * カートの永続化のポート。ユースケース（アプリケーション層）が必要とする I/O の抽象で、ドメイン語彙
 * ではない。だから domain ではなくアプリケーション層に置く。実装はインフラ層。
 *
 * <p>この設計では {@link #loadCart(UUID)} が全アイテムをロードして完全な {@link Cart} を組み立てる。
 * 不変条件チェックがドメイン内で完結する代わりに、ここが性能上の弱点になる。
 */
public interface CartRepository {
    Cart loadCart(UUID userId);

    void saveCart(UUID userId, Cart cart);
}
