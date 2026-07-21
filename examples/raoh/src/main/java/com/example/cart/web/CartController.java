package com.example.cart.web;

import static net.unit8.raoh.decode.ObjectDecoders.string;

import com.example.cart.domain.AddItemToCart;
import com.example.cart.domain.CartItem;
import com.example.cart.domain.CartQueryRepository;
import com.example.cart.domain.IssueQuote;
import com.example.cart.domain.Order;
import com.example.cart.domain.Orderer;
import com.example.cart.domain.Page;
import com.example.cart.domain.PlaceOrder;
import com.example.cart.domain.Quotation;
import com.example.cart.domain.UserId;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.unit8.raoh.Err;
import net.unit8.raoh.Issues;
import net.unit8.raoh.MessageResolver;
import net.unit8.raoh.Ok;
import net.unit8.raoh.Result;
import net.unit8.raoh.decode.Decoder;
import net.unit8.raoh.decode.combinator.Tuple2;
import net.unit8.raoh.decode.combinator.Tuple3;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * 全境界が集まる Web 層。入口では JSON を {@code switch} でデコードし（Ok/Err）、合成された振る舞い
 * {@link AddItemToCart} を呼ぶ。usecase クラスは無い——Controller は「decode して振る舞いに渡し、
 * 結果を HTTP に写す」だけ。
 *
 * <p>失敗は2段でどちらも値（{@link Result} の Err）。入力の型・形式エラー（decode 失敗）は 400、
 * 業務エラー（{@code sale_ended} / {@code cart_full} / {@code product_not_found}）は 422。
 */
@RestController
@RequestMapping("/carts")
public class CartController {

    /** クエリパラメータの userId（生の String）を UserId にデコードする境界 decoder。 */
    private static final Decoder<@Nullable Object, UserId> USER_ID =
            string().uuid().map(UserId::new);

    private final AddItemToCart addItemToCart;
    private final PlaceOrder placeOrder;
    private final IssueQuote issueQuote;
    private final CartQueryRepository cartQuery;
    private final TransactionTemplate tx;

    public CartController(AddItemToCart addItemToCart,
                          PlaceOrder placeOrder,
                          IssueQuote issueQuote,
                          CartQueryRepository cartQuery,
                          PlatformTransactionManager txManager) {
        this.addItemToCart = addItemToCart;
        this.placeOrder = placeOrder;
        this.issueQuote = issueQuote;
        this.cartQuery = cartQuery;
        this.tx = new TransactionTemplate(txManager);
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(@RequestBody JsonNode body, Locale locale) {
        return switch (JsonCartDecoders.ADD_ITEM.decode(body)) {
            // decode 成功 → Tuple3 をここで分解し、トランザクションを張ってドメインの振る舞いを起動する。
            // 型引数は switch のセレクタ型から推論されるので Ok/Tuple3 に <...> は要らない。
            case Ok(Tuple3(var userId, var productId, var quantity)) ->
                    respondTo(tx.execute(status -> addItemToCart.apply(userId, productId, quantity)), locale);
            // decode 失敗（不正 UUID・非正の数量など） → 400。
            case Err(var issues) ->
                    ResponseEntity.badRequest().body(errorBody(issues, locale));
        };
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody JsonNode body, Locale locale) {
        return switch (JsonCartDecoders.CHECKOUT.decode(body)) {
            // decode 成功 → Tuple2 を分解し、トランザクションを張って PlaceOrder を起動する。
            case Ok(Tuple2(var userId, var orderer)) ->
                    respondToOrder(tx.execute(status -> placeOrder.apply(userId, orderer)), locale);
            case Err(var issues) ->
                    ResponseEntity.badRequest().body(errorBody(issues, locale));
        };
    }

    @PostMapping("/quote")
    public ResponseEntity<?> quote(@RequestBody JsonNode body, Locale locale) {
        return switch (JsonCartDecoders.CHECKOUT.decode(body)) {
            case Ok(Tuple2(var userId, var orderer)) -> switch (orderer) {
                // 見積は法人限定。sealed なので、個人を型として弾ける。
                case Orderer.Corporation corporation ->
                        respondToQuote(issueQuote.apply(userId, corporation), locale);
                case Orderer.Individual ignored ->
                        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(Map.of("error", "見積は法人のみ発行できます"));
            };
            case Err(var issues) ->
                    ResponseEntity.badRequest().body(errorBody(issues, locale));
        };
    }

    @GetMapping("/items")
    public ResponseEntity<?> listItems(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Locale locale) {
        // 一覧は業務ルールが無いので gateway を直接呼ぶ（合成する振る舞いが無い）。
        return switch (USER_ID.decode(userId)) {
            case Ok(var uid) -> {
                Page<CartItem> result = cartQuery.findItemsByPage(uid, page, size);
                yield ResponseEntity.ok(CartViewEncoders.pageView(result));
            }
            case Err(var issues) -> ResponseEntity.badRequest().body(errorBody(issues, locale));
        };
    }

    /** 振る舞いの結果を HTTP ステータスへ写す。Ok なら 201、業務 Err なら 422。 */
    private static ResponseEntity<?> respondTo(Result<CartItem> result, Locale locale) {
        return switch (result) {
            case Ok(var ignored) -> ResponseEntity.status(HttpStatus.CREATED).build();
            case Err(var issues) ->
                    ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorBody(issues, locale));
        };
    }

    /** 注文の結果を HTTP へ写す。Ok なら 201 と注文内容、業務 Err（空カート等）なら 422。 */
    private static ResponseEntity<?> respondToOrder(Result<Order> result, Locale locale) {
        return switch (result) {
            case Ok(var order) ->
                    ResponseEntity.status(HttpStatus.CREATED).body(OrderViewEncoders.orderView(order));
            case Err(var issues) ->
                    ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorBody(issues, locale));
        };
    }

    /** 見積の結果を HTTP へ写す。Ok なら 200 と見積内容、業務 Err（空カート等）なら 422。 */
    private static ResponseEntity<?> respondToQuote(Result<Quotation> result, Locale locale) {
        return switch (result) {
            case Ok(var quote) -> ResponseEntity.ok(OrderViewEncoders.quotationView(quote));
            case Err(var issues) ->
                    ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorBody(issues, locale));
        };
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
