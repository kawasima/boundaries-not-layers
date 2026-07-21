package com.example.cart.usecase;

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
 * チェックアウトのユースケース（性能+純粋性版）。
 *
 * <p>アプリ層として I/O を担い、純粋な {@link CheckoutService} に渡す。この例は完全性を犠牲にするので
 * カートは anemic だが、チェックアウトには全明細が要るので {@code findAllItems} で取り出す。ドメイン側は
 * リポジトリに触れない（純粋性）。
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
        List<CartItem> items = cartRepository.findAllItems(userId);

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
