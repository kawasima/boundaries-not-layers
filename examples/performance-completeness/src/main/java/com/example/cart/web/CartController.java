package com.example.cart.web;

import com.example.cart.domain.CartItem;
import com.example.cart.domain.CheckoutService;
import com.example.cart.domain.Order;
import com.example.cart.domain.Orderer;
import com.example.cart.domain.OrdererType;
import com.example.cart.domain.Page;
import com.example.cart.domain.Quotation;
import com.example.cart.usecase.AddItemToCartCommand;
import com.example.cart.usecase.AddItemToCartUseCase;
import com.example.cart.usecase.ListCartItemsUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/carts")
public class CartController {

    private final AddItemToCartUseCase addItemToCartUseCase;
    private final ListCartItemsUseCase listCartItemsUseCase;
    private final CheckoutService checkoutService;

    public CartController(AddItemToCartUseCase addItemToCartUseCase,
                          ListCartItemsUseCase listCartItemsUseCase,
                          CheckoutService checkoutService) {
        this.addItemToCartUseCase = addItemToCartUseCase;
        this.listCartItemsUseCase = listCartItemsUseCase;
        this.checkoutService = checkoutService;
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItem(@RequestBody AddItemToCartRequest request) {
        addItemToCartUseCase.handle(new AddItemToCartCommand(
                request.userId(), request.productId(), request.quantity()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/items")
    public Page<CartItem> listItems(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return listCartItemsUseCase.handle(userId, page, size);
    }

    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(@Valid @RequestBody CheckoutRequest request) {
        Orderer orderer = toOrderer(request.orderer());
        Order order = checkoutService.checkout(UUID.fromString(request.userId()), orderer);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PostMapping("/quote")
    public ResponseEntity<Quotation> quote(@Valid @RequestBody CheckoutRequest request) {
        // 見積は永続化せず返すだけ。法人限定の判定は CheckoutService（区分の実行時チェック）に任せる。
        Orderer orderer = toOrderer(request.orderer());
        Quotation quotation = checkoutService.issueQuote(UUID.fromString(request.userId()), orderer);
        return ResponseEntity.ok(quotation);
    }

    private static Orderer toOrderer(OrdererForm form) {
        OrdererType type = "individual".equals(form.type()) ? OrdererType.INDIVIDUAL : OrdererType.CORPORATION;
        return new Orderer(type, form.email(), form.name(), form.companyName(), form.corporateNumber());
    }
}
