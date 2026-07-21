package com.example.cart;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * raoh-souther 版のエンドツーエンドテスト（実 H2 + jOOQ）。raoh 版と同じ外形（成功=201 / 上限=422 /
 * 販売終了=422 / 不正入力=400）を満たす。ドメインは Souther 生成だが、HTTP 契約と DB 効果は raoh 版と同一。
 * 上限は cart.sou の {@code addToCart} が持つ 10000。
 */
@SpringBootTest
class CartIntegrationTest {

    static final String USER = "11111111-1111-1111-1111-111111111111";
    static final String ON_SALE = "33333333-3333-3333-3333-333333333333";
    static final String OFF_SALE = "44444444-4444-4444-4444-444444444444";

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private String body(String userId, String productId, int quantity) {
        return """
                {"userId":"%s","productId":"%s","quantity":%d}
                """.formatted(userId, productId, quantity);
    }

    @Test
    void 販売中の商品を追加できる() throws Exception {
        mockMvc.perform(post("/carts/items").contentType(MediaType.APPLICATION_JSON)
                        .content(body(USER, ON_SALE, 8)))
                .andExpect(status().isCreated());
    }

    @Test
    void 上限を超えると422() throws Exception {
        mockMvc.perform(post("/carts/items").contentType(MediaType.APPLICATION_JSON)
                        .content(body(USER, ON_SALE, 10001)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void 販売終了商品は422() throws Exception {
        mockMvc.perform(post("/carts/items").contentType(MediaType.APPLICATION_JSON)
                        .content(body(USER, OFF_SALE, 1)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void 不正なUUIDは400() throws Exception {
        mockMvc.perform(post("/carts/items").contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-a-uuid", ON_SALE, 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 数量が0以下なら400() throws Exception {
        mockMvc.perform(post("/carts/items").contentType(MediaType.APPLICATION_JSON)
                        .content(body(USER, ON_SALE, 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 追加した商品が一覧に出る() throws Exception {
        mockMvc.perform(post("/carts/items").contentType(MediaType.APPLICATION_JSON)
                        .content(body(USER, ON_SALE, 3)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/carts/items").param("userId", USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.productId=='" + ON_SALE + "')]").exists());
    }

    private String individualOrderer() {
        return "{\"type\":\"individual\",\"email\":\"taro@example.com\",\"name\":\"山田太郎\"}";
    }

    private String corporationOrderer() {
        return "{\"type\":\"corporation\",\"email\":\"info@acme.co.jp\","
                + "\"companyName\":\"Acme株式会社\",\"corporateNumber\":\"1234567890123\"}";
    }

    private String checkout(String userId, String ordererJson) {
        return "{\"userId\":\"%s\",\"orderer\":%s}".formatted(userId, ordererJson);
    }

    @Test
    void 個人の注文者でチェックアウトできる() throws Exception {
        String user = "11111111-1111-1111-1111-111111111112";
        // 1200 円 × 8 = 9600（5000 以上なので 10% 引き → 割引 960、合計 8640）
        mockMvc.perform(post("/carts/items").contentType(MediaType.APPLICATION_JSON)
                        .content(body(user, ON_SALE, 8)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/carts/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(user, individualOrderer())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderer.type").value("individual"))
                .andExpect(jsonPath("$.orderer.name").value("山田太郎"))
                .andExpect(jsonPath("$.subtotal").value(9600))
                .andExpect(jsonPath("$.discount").value(960))
                .andExpect(jsonPath("$.total").value(8640))
                .andExpect(jsonPath("$.lines[0].productId").value(ON_SALE));
    }

    @Test
    void 法人の注文者でチェックアウトできる() throws Exception {
        String user = "11111111-1111-1111-1111-111111111114";
        mockMvc.perform(post("/carts/items").contentType(MediaType.APPLICATION_JSON)
                        .content(body(user, ON_SALE, 3)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/carts/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(user, corporationOrderer())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderer.type").value("corporation"))
                .andExpect(jsonPath("$.orderer.companyName").value("Acme株式会社"))
                .andExpect(jsonPath("$.orderer.corporateNumber").value("1234567890123"));
    }

    @Test
    void 法人番号が13桁でなければ400() throws Exception {
        String user = "11111111-1111-1111-1111-111111111115";
        mockMvc.perform(post("/carts/items").contentType(MediaType.APPLICATION_JSON)
                        .content(body(user, ON_SALE, 1)))
                .andExpect(status().isCreated());

        String badCorp = "{\"type\":\"corporation\",\"email\":\"info@acme.co.jp\","
                + "\"companyName\":\"Acme株式会社\",\"corporateNumber\":\"12345\"}";
        mockMvc.perform(post("/carts/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(user, badCorp)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 空カートのチェックアウトは422() throws Exception {
        // アイテムを一度も入れていない専用ユーザー。
        String user = "11111111-1111-1111-1111-111111111113";
        mockMvc.perform(post("/carts/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(user, individualOrderer())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void 法人は見積を発行できる() throws Exception {
        String user = "11111111-1111-1111-1111-111111111116";
        mockMvc.perform(post("/carts/items").contentType(MediaType.APPLICATION_JSON)
                        .content(body(user, ON_SALE, 8)))
                .andExpect(status().isCreated());

        // 金額は注文と同じ計算（9600 → 割引 960 → 合計 8640）。加えて有効期限が付く。
        mockMvc.perform(post("/carts/quote").contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(user, corporationOrderer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId").exists())
                .andExpect(jsonPath("$.orderer.type").value("corporation"))
                .andExpect(jsonPath("$.subtotal").value(9600))
                .andExpect(jsonPath("$.discount").value(960))
                .andExpect(jsonPath("$.total").value(8640))
                .andExpect(jsonPath("$.validUntil").exists());
    }

    @Test
    void 個人は見積を発行できず422() throws Exception {
        String user = "11111111-1111-1111-1111-111111111117";
        mockMvc.perform(post("/carts/items").contentType(MediaType.APPLICATION_JSON)
                        .content(body(user, ON_SALE, 2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/carts/quote").contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(user, individualOrderer())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void 空カートの見積は422() throws Exception {
        String user = "11111111-1111-1111-1111-111111111118";
        mockMvc.perform(post("/carts/quote").contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(user, corporationOrderer())))
                .andExpect(status().isUnprocessableEntity());
    }
}
