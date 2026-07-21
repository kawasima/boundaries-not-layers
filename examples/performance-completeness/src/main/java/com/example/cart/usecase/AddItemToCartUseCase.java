package com.example.cart.usecase;

import com.example.cart.domain.CartForUpdate;
import com.example.cart.domain.CartWriteRepository;
import com.example.cart.domain.ProductRepository;
import com.example.cart.domain.SaleEndedException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * カートに商品を入れるユースケース（性能+完全性版）。
 *
 * <p>上限チェックはドメイン（{@link CartForUpdate#add}）が持つ。ただしドメインは
 * 全アイテムを持たず、リポジトリへ合計数量を問い合わせて判断する。ユースケースは
 * ロードした CartForUpdate に、更新用リポジトリを渡して add を呼ぶだけ。
 */
@Service
public class AddItemToCartUseCase {

    private final CartWriteRepository cartRepository;
    private final ProductRepository productRepository;

    public AddItemToCartUseCase(CartWriteRepository cartRepository, ProductRepository productRepository) {
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

        CartForUpdate cart = cartRepository.loadCart(userId);
        // 上限チェックと追加は、ドメインがリポジトリ越しに行う。
        cart.add(productId, quantity, cartRepository);
    }

    private static int requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return quantity;
    }
}
