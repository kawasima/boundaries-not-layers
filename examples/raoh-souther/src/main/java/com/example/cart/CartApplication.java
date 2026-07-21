package com.example.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * raoh 版カート例を Souther 生成ドメインへ移植したアプリケーション。ドメイン型は
 * {@code src/main/souther/cart.sou} から生成され（{@code com.example.cart.domain}）、
 * 境界（HTTP/DB）は raoh で decode・encode する。
 */
@SpringBootApplication
public class CartApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }
}
