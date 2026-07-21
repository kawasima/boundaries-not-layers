package com.example.cart.web;

import static net.unit8.raoh.decode.ObjectDecoders.string;

import com.example.cart.domain.AddToCart;
import com.example.cart.domain.Cart;
import com.example.cart.domain.CartFull;
import com.example.cart.domain.CartItem;
import com.example.cart.domain.Corporation;
import com.example.cart.domain.EmptyCart;
import com.example.cart.domain.Individual;
import com.example.cart.domain.IssueQuote;
import com.example.cart.domain.Order;
import com.example.cart.domain.OrderId;
import com.example.cart.domain.OrderLine;
import com.example.cart.domain.Orderer;
import com.example.cart.domain.PlaceOrder;
import com.example.cart.domain.PriceLine;
import com.example.cart.domain.Product;
import com.example.cart.domain.Quotation;
import com.example.cart.domain.QuoteId;
import com.example.cart.domain.SaleEnded;
import com.example.cart.domain.UserId;
import com.example.cart.infrastructure.CartQueryRepository;
import com.example.cart.infrastructure.CartRepository;
import com.example.cart.infrastructure.OrderRepository;
import com.example.cart.infrastructure.Page;
import com.example.cart.infrastructure.ProductRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.Err;
import net.unit8.raoh.Issues;
import net.unit8.raoh.MessageResolver;
import net.unit8.raoh.Ok;
import net.unit8.raoh.Path;
import net.unit8.raoh.Result;
import net.unit8.raoh.decode.Decoder;
import net.unit8.raoh.decode.combinator.Tuple2;
import net.unit8.raoh.decode.combinator.Tuple3;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * 全境界が集まる Web 層。JSON を raoh で {@code switch} デコード（Ok/Err）し、Souther が生成した純粋な
 * ドメイン振る舞い（{@link AddToCart} / {@link PriceLine} / {@link PlaceOrder} / {@link IssueQuote}）を
 * 呼ぶ。振る舞いは I/O を持たないので、Controller が gateway でカート・商品を読み、結果を書く。
 *
 * <p>失敗は2段。入力の型・形式エラー（decode 失敗）は 400、業務エラー（{@code sale_ended} /
 * {@code cart_full} / {@code empty_cart} / {@code product_not_found}）は 422。前者は raoh の Err、
 * 後者は振る舞いが返す sum の失敗ケース（{@link SaleEnded} / {@link CartFull} / {@link EmptyCart}）。
 */
@RestController
@RequestMapping("/carts")
public class CartController {

    /** クエリパラメータの userId（生の String）を検証し UserId に組む境界 decoder。 */
    private static final Decoder<@Nullable Object, UserId> USER_ID =
            string().uuid().map(UUID::toString)
                    .flatMap(s -> UserId.decoder().decode(s, Path.ROOT));

    private final AddToCart addToCart;
    private final PriceLine priceLine;
    private final PlaceOrder placeOrder;
    private final IssueQuote issueQuote;
    private final ProductRepository products;
    private final CartRepository carts;
    private final CartQueryRepository cartQuery;
    private final OrderRepository orders;
    private final TransactionTemplate tx;

    public CartController(AddToCart addToCart,
                          PriceLine priceLine,
                          PlaceOrder placeOrder,
                          IssueQuote issueQuote,
                          ProductRepository products,
                          CartRepository carts,
                          CartQueryRepository cartQuery,
                          OrderRepository orders,
                          TransactionTemplate tx) {
        this.addToCart = addToCart;
        this.priceLine = priceLine;
        this.placeOrder = placeOrder;
        this.issueQuote = issueQuote;
        this.products = products;
        this.carts = carts;
        this.cartQuery = cartQuery;
        this.orders = orders;
        this.tx = tx;
    }

    @PostMapping("/items")
    public ResponseEntity<Object> addItem(@RequestBody JsonNode body, Locale locale) {
        return switch (JsonCartDecoders.ADD_ITEM.decode(body)) {
            case Ok(Tuple3(var userId, var productId, var quantity)) ->
                    tx.execute(status -> switch (products.load(productId)) {
                        case Ok(var product) -> switch (carts.loadForUpdate(userId)) {
                            case Ok(var cart) -> switch (addToCart.apply(cart, product, quantity)) {
                                case CartItem item -> {
                                    carts.addItem(cart.id(), item);
                                    yield created(null);
                                }
                                case SaleEnded ignored -> unprocessable(Map.of("error", "sale_ended"));
                                case CartFull ignored -> unprocessable(Map.of("error", "cart_full"));
                                default -> throw new IllegalStateException("unexpected addToCart result");
                            };
                            case Err(var issues) -> unprocessable(errorBody(issues, locale));
                        };
                        case Err(var issues) -> unprocessable(errorBody(issues, locale));
                    });
            case Err(var issues) -> badRequest(errorBody(issues, locale));
        };
    }

    @PostMapping("/checkout")
    public ResponseEntity<Object> checkout(@RequestBody JsonNode body, Locale locale) {
        return switch (JsonCartDecoders.CHECKOUT.decode(body)) {
            case Ok(Tuple2(var userId, var orderer)) -> tx.execute(status -> {
                Priced priced = priceLines(userId, locale);
                if (priced.failure() != null) {
                    return priced.failure();
                }
                return switch (placeOrder.apply(newOrderId(), userId, orderer, priced.lines())) {
                    case Order order -> {
                        orders.save(order);
                        yield created(OrderViewEncoders.orderView(order));
                    }
                    case EmptyCart ignored -> unprocessable(Map.of("error", "empty_cart"));
                    default -> throw new IllegalStateException("unexpected placeOrder result");
                };
            });
            case Err(var issues) -> badRequest(errorBody(issues, locale));
        };
    }

    @PostMapping("/quote")
    public ResponseEntity<Object> quote(@RequestBody JsonNode body, Locale locale) {
        return switch (JsonCartDecoders.CHECKOUT.decode(body)) {
            case Ok(Tuple2(var userId, var orderer)) -> switch (orderer) {
                // 見積は法人限定。sealed なので個人を型で弾ける。
                case Corporation corporation -> {
                    Priced priced = priceLines(userId, locale);
                    if (priced.failure() != null) {
                        yield priced.failure();
                    }
                    String validUntil = LocalDate.now().plusDays(30).toString();
                    yield switch (issueQuote.apply(newQuoteId(), userId, corporation, priced.lines(), validUntil)) {
                        case Quotation q -> ResponseEntity.ok((Object) OrderViewEncoders.quotationView(q));
                        case EmptyCart ignored -> unprocessable(Map.of("error", "empty_cart"));
                        default -> throw new IllegalStateException("unexpected issueQuote result");
                    };
                }
                case Individual ignored ->
                        unprocessable(Map.of("error", "見積は法人のみ発行できます"));
            };
            case Err(var issues) -> badRequest(errorBody(issues, locale));
        };
    }

    @GetMapping("/items")
    public ResponseEntity<Object> listItems(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Locale locale) {
        return switch (USER_ID.decode(userId)) {
            case Ok(var uid) -> {
                Page<CartItem> result = cartQuery.findItemsByPage(uid, page, size);
                yield ResponseEntity.ok((Object) CartViewEncoders.pageView(result));
            }
            case Err(var issues) -> badRequest(errorBody(issues, locale));
        };
    }

    /**
     * カート全明細を価格つき明細（{@link OrderLine}）へ変換する。各商品を gateway で読み、純粋な
     * {@link PriceLine} で販売中を確かめて価格を載せる。販売終了・商品なしは 422 の応答へ短絡する。
     */
    private Priced priceLines(UserId userId, Locale locale) {
        List<OrderLine> lines = new ArrayList<>();
        for (CartItem item : cartQuery.findAllItems(userId)) {
            switch (products.load(item.productId())) {
                case Ok(var product) -> {
                    switch (priceLine.apply(product, item.quantity())) {
                        case OrderLine line -> lines.add(line);
                        case SaleEnded ignored -> {
                            return Priced.failure(unprocessable(Map.of("error", "sale_ended")));
                        }
                        default -> throw new IllegalStateException("unexpected priceLine result");
                    }
                }
                case Err(var issues) -> {
                    return Priced.failure(unprocessable(errorBody(issues, locale)));
                }
            }
        }
        return Priced.ok(lines);
    }

    /** 価格付けの結果。成功なら {@code lines}、失敗なら 422 応答 {@code failure}。片方だけが非 null。 */
    private record Priced(@Nullable List<OrderLine> lines, @Nullable ResponseEntity<Object> failure) {
        static Priced ok(List<OrderLine> lines) {
            return new Priced(lines, null);
        }

        static Priced failure(ResponseEntity<Object> response) {
            return new Priced(null, response);
        }
    }

    private static OrderId newOrderId() {
        return unwrap(OrderId.decoder().decode(UUID.randomUUID().toString(), Path.ROOT));
    }

    private static QuoteId newQuoteId() {
        return unwrap(QuoteId.decoder().decode(UUID.randomUUID().toString(), Path.ROOT));
    }

    private static <T> T unwrap(Result<T> result) {
        return switch (result) {
            case Ok(var value) -> value;
            case Err(var issues) -> throw new IllegalStateException("unexpected decode failure: " + issues);
        };
    }

    private static ResponseEntity<Object> created(@Nullable Object body) {
        var builder = ResponseEntity.status(HttpStatus.CREATED);
        return body == null ? builder.build() : builder.body(body);
    }

    private static ResponseEntity<Object> unprocessable(Object body) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    private static ResponseEntity<Object> badRequest(Object body) {
        return ResponseEntity.badRequest().body(body);
    }

    /** raoh の Issues を、パスつきの構造化エラー本文にする。 */
    private static Map<String, @Nullable Object> errorBody(Issues issues, Locale locale) {
        var resolved = issues.resolve(MessageResolver.DEFAULT, locale);
        var body = new LinkedHashMap<String, @Nullable Object>();
        body.put("issues", resolved.toJsonList());
        body.put("errors", resolved.flatten());
        return body;
    }
}
