package com.example.cart.config;

import com.example.cart.domain.AddItemToCart;
import com.example.cart.domain.IssueQuote;
import com.example.cart.domain.LoadCart;
import com.example.cart.domain.LoadProduct;
import com.example.cart.domain.PlaceOrder;
import com.example.cart.domain.PriceCart;
import com.example.cart.domain.SaveItem;
import com.example.cart.domain.SaveOrder;
import com.example.cart.infrastructure.JooqLoadCart;
import com.example.cart.infrastructure.JooqLoadProduct;
import com.example.cart.infrastructure.JooqPriceCart;
import com.example.cart.infrastructure.JooqSaveItem;
import com.example.cart.infrastructure.JooqSaveOrder;
import org.jooq.DSLContext;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Souther 生成の振る舞いを Spring に配線する。DataSource / DSLContext / TransactionManager と
 * schema.sql の実行は Spring Boot autoconfig（spring-boot-starter-jooq + H2）に任せる。
 *
 * <p>INJECTED 振る舞い（{@link LoadProduct} / {@link LoadCart} / {@link SaveItem} / {@link PriceCart} /
 * {@link SaveOrder}）は jOOQ 実装を基底型で公開し、COMPOSED 振る舞いは {@code bind(...)} で合成する。
 */
@Configuration(proxyBeanMethods = false)
public class CartConfig {

    /**
     * jOOQ の識別子クォートを止める。無引用の名前は H2 が大文字化するので、コード中の小文字名
     * （cart / cart_item / product / orders / order_line）がスキーマの大文字名に一致する。
     */
    @Bean
    public Settings jooqSettings() {
        return new Settings().withRenderQuotedNames(RenderQuotedNames.NEVER);
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager tm) {
        return new TransactionTemplate(tm);
    }

    @Bean
    public LoadProduct loadProduct(DSLContext dsl) {
        return new JooqLoadProduct(dsl);
    }

    @Bean
    public LoadCart loadCart(DSLContext dsl) {
        return new JooqLoadCart(dsl);
    }

    @Bean
    public SaveItem saveItem(DSLContext dsl) {
        return new JooqSaveItem(dsl);
    }

    @Bean
    public PriceCart priceCart(DSLContext dsl) {
        return new JooqPriceCart(dsl);
    }

    @Bean
    public SaveOrder saveOrder(DSLContext dsl) {
        return new JooqSaveOrder(dsl);
    }

    @Bean
    public AddItemToCart addItemToCart(LoadProduct loadProduct, LoadCart loadCart, SaveItem saveItem) {
        return AddItemToCart.bind(loadProduct, loadCart, saveItem);
    }

    @Bean
    public PlaceOrder placeOrder(PriceCart priceCart, SaveOrder saveOrder) {
        return PlaceOrder.bind(priceCart, saveOrder);
    }

    @Bean
    public IssueQuote issueQuote(PriceCart priceCart) {
        return IssueQuote.bind(priceCart);
    }
}
