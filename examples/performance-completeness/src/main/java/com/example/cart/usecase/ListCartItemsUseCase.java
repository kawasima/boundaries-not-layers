package com.example.cart.usecase;

import com.example.cart.domain.Cart;
import com.example.cart.domain.CartItem;
import com.example.cart.domain.CartReadRepository;
import com.example.cart.domain.Page;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * カートの中身をページ単位で参照するユースケース。
 * 更新側（{@link AddItemToCartUseCase}）と読み取り側を分けている。
 */
@Service
public class ListCartItemsUseCase {

    private final CartReadRepository cartRepository;

    public ListCartItemsUseCase(CartReadRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Transactional(readOnly = true)
    public Page<CartItem> handle(String userId, int page, int size) {
        Cart cart = new Cart(UUID.fromString(userId));
        return cart.items(page, size, cartRepository);
    }
}
