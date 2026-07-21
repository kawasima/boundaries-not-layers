package com.example.cart.domain;

/**
 * 注文者。個人と法人で入力内容が違うので、型そのものを分ける。
 *
 * <p>UserId のプロフィールは注文フォームの入力補完にしか使わない。チェックアウト時には注文者情報が
 * 丸ごと飛んでくるので、それを個人／法人それぞれの型に decode し分ける。sealed なので、
 * 「法人なのに法人番号が無い」「個人なのに会社名がある」といった不正な組み合わせは、そもそも
 * 型として存在できない。分岐は {@code switch} が網羅を保証する。
 */
public sealed interface Orderer permits Orderer.Individual, Orderer.Corporation {

    Email email();

    /** 個人。氏名を持つ。 */
    record Individual(Email email, String name) implements Orderer {}

    /** 法人。会社名と法人番号（13桁）を持つ。 */
    record Corporation(Email email, String companyName, String corporateNumber) implements Orderer {}
}
