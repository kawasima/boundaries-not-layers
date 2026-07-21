package com.example.cart.domain;

import java.util.UUID;

/**
 * ユーザーの識別子。
 *
 * <p>値オブジェクトはこのように「素の record」にとどめる。UUID 形式かどうかの検証は
 * 境界の decoder（{@code JsonCartDecoders} / {@code JooqCartDecoders}）が行い、
 * デコードに成功したあとでのみこの record が構築される。これが parse, don't validate の形。
 */
public record UserId(UUID value) {
}
