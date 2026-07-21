package com.example.cart.web;

import static net.unit8.raoh.decode.ObjectDecoders.string;

import com.example.cart.domain.AddItemToCart;
import com.example.cart.domain.CartFull;
import com.example.cart.domain.CartItem;
import com.example.cart.domain.Corporation;
import com.example.cart.domain.EmptyCart;
import com.example.cart.domain.Individual;
import com.example.cart.domain.IssueQuote;
import com.example.cart.domain.ItemAdded;
import com.example.cart.domain.OrderId;
import com.example.cart.domain.OrderPlaced;
import com.example.cart.domain.PlaceOrder;
import com.example.cart.domain.ProductNotFound;
import com.example.cart.domain.QuoteId;
import com.example.cart.domain.Quotation;
import com.example.cart.domain.SaleEnded;
import com.example.cart.domain.UserId;
import com.example.cart.infrastructure.CartQueryRepository;
import com.example.cart.infrastructure.Page;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
 * 全境界が集まる Web 層。JSON を raoh で {@code switch} デコード（Ok/Err）し、Souther の COMPOSED 振る舞い
 * （{@link AddItemToCart} / {@link PlaceOrder} / {@link IssueQuote}）を 1 回呼ぶ。合成された振る舞いは
 * 多引数なので、decode した値をそのまま {@code apply(...)} へ渡す（コマンド record は要らない）。DB 参照・
 * 書き込みは振る舞いに INJECTED された gateway が担うので、Controller は薄い。
 *
 * <p>失敗は2段。入力の型・形式エラー（decode 失敗）は 400、業務エラーは 422。前者は raoh の Err、後者は
 * 振る舞いが返す sum の失敗ケース（{@link SaleEnded} / {@link CartFull} / {@link EmptyCart} /
 * {@link ProductNotFound}）。
 */
@RestController
@RequestMapping("/carts")
public class CartController {

    /** クエリパラメータの userId（生の String）を検証し UserId に組む境界 decoder。 */
    private static final Decoder<@Nullable Object, UserId> USER_ID =
            string().uuid().map(UUID::toString)
                    .flatMap(s -> UserId.decoder().decode(s, Path.ROOT));

    private final AddItemToCart addItemToCart;
    private final PlaceOrder placeOrder;
    private final IssueQuote issueQuote;
    private final CartQueryRepository cartQuery;
    private final TransactionTemplate tx;

    public CartController(AddItemToCart addItemToCart,
                          PlaceOrder placeOrder,
                          IssueQuote issueQuote,
                          CartQueryRepository cartQuery,
                          TransactionTemplate tx) {
        this.addItemToCart = addItemToCart;
        this.placeOrder = placeOrder;
        this.issueQuote = issueQuote;
        this.cartQuery = cartQuery;
        this.tx = tx;
    }

    @PostMapping("/items")
    public ResponseEntity<Object> addItem(@RequestBody JsonNode body, Locale locale) {
        return switch (JsonCartDecoders.ADD_ITEM.decode(body)) {
            case Ok(Tuple3(var userId, var productId, var quantity)) ->
                    tx.execute(status -> switch (addItemToCart.apply(userId, productId, quantity)) {
                        case ItemAdded _ -> created(null);
                        case ProductNotFound _ -> unprocessable("product_not_found");
                        case SaleEnded _ -> unprocessable("sale_ended");
                        case CartFull _ -> unprocessable("cart_full");
                    });
            case Err(var issues) -> badRequest(errorBody(issues, locale));
        };
    }

    @PostMapping("/checkout")
    public ResponseEntity<Object> checkout(@RequestBody JsonNode body, Locale locale) {
        return switch (JsonCartDecoders.CHECKOUT.decode(body)) {
            case Ok(Tuple2(var userId, var orderer)) -> {
                OrderId orderId = newOrderId();
                yield tx.execute(status -> switch (placeOrder.apply(orderId, userId, orderer)) {
                    case OrderPlaced placed -> created(OrderViewEncoders.orderView(placed.order()));
                    case EmptyCart _ -> unprocessable("empty_cart");
                    case SaleEnded _ -> unprocessable("sale_ended");
                    case ProductNotFound _ -> unprocessable("product_not_found");
                });
            }
            case Err(var issues) -> badRequest(errorBody(issues, locale));
        };
    }

    @PostMapping("/quote")
    public ResponseEntity<Object> quote(@RequestBody JsonNode body, Locale locale) {
        return switch (JsonCartDecoders.CHECKOUT.decode(body)) {
            case Ok(Tuple2(var userId, var orderer)) -> switch (orderer) {
                case Corporation _ -> {
                    String validUntil = LocalDate.now().plusDays(30).toString();
                    yield switch (issueQuote.apply(newQuoteId(), userId, orderer, validUntil)) {
                        case Quotation quotation ->
                                ResponseEntity.ok(OrderViewEncoders.quotationView(quotation));
                        case EmptyCart _ -> unprocessable("empty_cart");
                        case SaleEnded _ -> unprocessable("sale_ended");
                        case ProductNotFound _ -> unprocessable("product_not_found");
                    };
                }
                case Individual ignored -> unprocessable(Map.of("error", "見積は法人のみ発行できます"));
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

    /** 採番した UUID を OrderId に組む（値オブジェクトに public コンストラクタが無いので decoder 経由）。 */
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

    private static ResponseEntity<Object> unprocessable(String errorCode) {
        return unprocessable(Map.of("error", errorCode));
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
