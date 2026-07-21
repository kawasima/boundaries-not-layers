package com.example.cart.domain;

/**
 * 注文者。個人・法人で型を分けず、区分（{@link OrdererType}）と、両方の項目をぜんぶ持つフラットな1型で表す。
 *
 * <p>ここが sealed 版（raoh）との差。区分は本来「境界（入力/保存）で個人か法人かを見分けるためのタグ」で
 * あって、ドメインの状態ではない。だが型を分けないと、その区分をドメインまで引きずり込むしかなくなる。
 * さらに {@code name}（個人用）と {@code companyName}/{@code corporateNumber}（法人用）が同居し、どれが
 * null かは {@code type} 次第。「法人なのに name が入っていて companyName が null」といった不正な状態も、
 * この型では表現できてしまう。防いでいるのは境界の Bean Validation だけで、型は守ってくれない。
 */
public record Orderer(
        OrdererType type,
        String email,
        String name,
        String companyName,
        String corporateNumber) {
}
