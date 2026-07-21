package com.example.cart.config;

import com.example.cart.domain.AddToCart;
import com.example.cart.domain.IssueQuote;
import com.example.cart.domain.PlaceOrder;
import com.example.cart.domain.PriceLine;
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
 * <p>振る舞いはすべて純粋（requires 無し）なので {@code Xxx.of()} で足りる。注入は不要。
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
    public AddToCart addToCart() {
        return AddToCart.of();
    }

    @Bean
    public PriceLine priceLine() {
        return PriceLine.of();
    }

    @Bean
    public PlaceOrder placeOrder() {
        return PlaceOrder.of();
    }

    @Bean
    public IssueQuote issueQuote() {
        return IssueQuote.of();
    }
}
