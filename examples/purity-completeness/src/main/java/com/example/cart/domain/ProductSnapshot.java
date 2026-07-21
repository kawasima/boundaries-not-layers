package com.example.cart.domain;

/**
 * チェックアウト時点でロード済みの商品状態（販売状態と単価）。
 *
 * <p>ドメインサービスを純粋に保つための入力。DB からの読み出しはアプリ層（use case）が行い、その結果を
 * この値にしてドメインサービスへ渡す。ドメイン側は外界に触れない。
 */
public record ProductSnapshot(boolean onSale, long price) {
}
