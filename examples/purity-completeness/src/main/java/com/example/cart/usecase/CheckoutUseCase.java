package com.example.cart.usecase;

import com.example.cart.domain.Cart;
import com.example.cart.domain.CartItem;
import com.example.cart.domain.CheckoutService;
import com.example.cart.domain.Order;
import com.example.cart.domain.Orderer;
import com.example.cart.domain.ProductSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * チェックアウトのユースケース（純粋性+完全性版）。
 *
 * <p>アプリ層として I/O を担う。カートと、明細にある商品の状態（販売状態・単価）をロードし、純粋な
 * {@link CheckoutService} に渡して注文を組み、保存する。ドメイン側はリポジトリに触れない（純粋性）。
 */
@Service
public class CheckoutUseCase {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CheckoutService checkoutService;

    public CheckoutUseCase(CartRepository cartRepository,
                           ProductRepository productRepository,
                           OrderRepository orderRepository,
                           CheckoutService checkoutService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.checkoutService = checkoutService;
    }

    @Transactional
    public Order handle(UUID userId, Orderer orderer) {
        Cart cart = cartRepository.loadCart(userId);
        List<CartItem> items = cart.items();

        // 明細にある商品の状態を先にロードして、純粋なドメインサービスへ渡す。
        Map<UUID, ProductSnapshot> products = new HashMap<>();
        for (CartItem item : items) {
            products.computeIfAbsent(item.productId(), pid ->
                    new ProductSnapshot(productRepository.isNowOnSale(pid), productRepository.priceOf(pid)));
        }

        Order order = checkoutService.checkout(userId, orderer, items, products);
        orderRepository.save(order);
        return order;
    }
}
