package com.example.cart.usecase;

import com.example.cart.domain.Cart;
import com.example.cart.domain.CartFullException;
import com.example.cart.domain.SaleEndedException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * カートに商品を入れるユースケース（性能+純粋性版）。
 *
 * <p>ドメイン（{@link Cart}）は anemic で、上限チェックを自分では行えない。そのため
 * 「合計数量 + 追加数量 &gt; 上限」の判定が、このユースケースに漏れ出している。
 * ドメインは純粋なまま、参照も集計クエリだけで高速。その代わり、完全性を犠牲にしている。
 */
@Service
public class AddItemToCartUseCase {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public AddItemToCartUseCase(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void handle(AddItemToCartCommand command) {
        UUID userId = UUID.fromString(command.userId());
        UUID productId = UUID.fromString(command.productId());
        int quantity = requirePositive(command.quantity());

        if (!productRepository.isNowOnSale(productId)) {
            throw new SaleEndedException();
        }

        Cart cart = cartRepository.loadCart(userId);

        // 本来 Cart が持つべき不変条件チェックが、ここ（ユースケース）に漏れている。
        if (cartRepository.getItemCount(cart.id()) + quantity > Cart.UPPER_BOUND) {
            throw new CartFullException();
        }

        cartRepository.addItem(cart.id(), productId, quantity);
    }

    private static int requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return quantity;
    }
}
