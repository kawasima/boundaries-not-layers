package com.example.cart.usecase;

import com.example.cart.domain.CartItem;
import com.example.cart.domain.CheckoutService;
import com.example.cart.domain.Orderer;
import com.example.cart.domain.ProductSnapshot;
import com.example.cart.domain.Quotation;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 見積発行のユースケース（性能+純粋性版）。アプリ層として I/O と時計を担い、純粋な
 * {@link CheckoutService#issueQuote} に渡す。金額計算は注文と同じだが、サービス側で重複している。
 */
@Service
public class QuoteUseCase {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CheckoutService checkoutService;

    public QuoteUseCase(CartRepository cartRepository,
                        ProductRepository productRepository,
                        CheckoutService checkoutService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.checkoutService = checkoutService;
    }

    @Transactional(readOnly = true)
    public Quotation handle(UUID userId, Orderer orderer) {
        List<CartItem> items = cartRepository.findAllItems(userId);

        Map<UUID, ProductSnapshot> products = new HashMap<>();
        for (CartItem item : items) {
            products.computeIfAbsent(item.productId(), pid ->
                    new ProductSnapshot(productRepository.isNowOnSale(pid), productRepository.priceOf(pid)));
        }

        return checkoutService.issueQuote(userId, orderer, items, products, LocalDate.now());
    }
}
