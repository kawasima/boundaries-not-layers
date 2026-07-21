package com.example.cart.usecase;

import com.example.cart.domain.AddItemToCartService;
import com.example.cart.domain.Cart;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * カートに商品を入れるユースケース（純粋性+完全性版）。
 *
 * <p>アプリ層として I/O を担う。境界の値（String）をドメインの型（UUID/int）へ変換し、リポジトリから
 * カートと商品の販売状態をロードし、純粋なドメインサービス {@link AddItemToCartService} に渡し、結果を
 * 保存する。ドメイン側はリポジトリに触れない（純粋性）。
 *
 * <p>{@code requirePositive} に注目。数量が正であることは業務の不変条件だが、{@code int} がそれを型で
 * 運ばないので、ここ（400 を返すため）でも、ドメインサービス側でも同じ検査を書く羽目になっている。
 * 値オブジェクト {@code Quantity} があれば、検査は構築時の一度で済み、以降は型が保証する。
 */
@Service
public class AddItemToCartUseCase {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final AddItemToCartService addItemToCartService;

    public AddItemToCartUseCase(CartRepository cartRepository,
                                ProductRepository productRepository,
                                AddItemToCartService addItemToCartService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.addItemToCartService = addItemToCartService;
    }

    @Transactional
    public void handle(AddItemToCartCommand command) {
        // String は UUID を保証しないのでここで parse（不正なら IllegalArgumentException → 400）。
        UUID userId = UUID.fromString(command.userId());
        UUID productId = UUID.fromString(command.productId());
        // 型が正数を保証しないので、境界でも確かめる。同じ検査がドメインサービスにもある。
        int quantity = requirePositive(command.quantity());

        // I/O はアプリ層で。ドメインサービスにはロード済みの値を渡す。
        boolean productOnSale = productRepository.isNowOnSale(productId);
        Cart cart = cartRepository.loadCart(userId);

        addItemToCartService.addItem(cart, productId, productOnSale, quantity);

        cartRepository.saveCart(userId, cart);
    }

    private static int requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return quantity;
    }
}
