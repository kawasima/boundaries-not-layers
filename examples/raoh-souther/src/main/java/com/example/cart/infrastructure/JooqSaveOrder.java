package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.Corporation;
import com.example.cart.domain.Individual;
import com.example.cart.domain.Order;
import com.example.cart.domain.OrderLine;
import com.example.cart.domain.OrderPlaced;
import com.example.cart.domain.SaveOrder;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.Path;
import org.jooq.DSLContext;

/**
 * {@code saveOrder} INJECTED 振る舞いの jOOQ 実装。注文ヘッダ1行と明細 N 行を書く。Souther の値からは
 * public アクセサで列値を読み出す。注文者は switch でフラットな列に落とす（個人／法人で埋まる列が変わる）。
 * 書き込んだ注文を {@code Order.encoder()} で Map 化し、{@link OrderPlaced} にデコードして返す。
 */
public final class JooqSaveOrder extends SaveOrder {

    private final DSLContext dsl;

    public JooqSaveOrder(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public OrderPlaced apply(Order order) {
        UUID orderId = UUID.fromString(order.id().value());
        UUID userId = UUID.fromString(order.userId().value());

        String ordererType;
        String email;
        String name = null;
        String companyName = null;
        String corporateNumber = null;
        switch (order.orderer()) {
            case Individual individual -> {
                ordererType = "individual";
                email = individual.email().value();
                name = individual.name();
            }
            case Corporation corporation -> {
                ordererType = "corporation";
                email = corporation.email().value();
                companyName = corporation.companyName();
                corporateNumber = corporation.corporateNumber();
            }
            default -> throw new IllegalStateException("unknown orderer: " + order.orderer());
        }

        dsl.insertInto(table("orders"))
                .columns(
                        field("order_id"), field("user_id"),
                        field("subtotal"), field("discount"), field("total"),
                        field("orderer_type"), field("orderer_email"),
                        field("orderer_name"), field("orderer_company_name"), field("orderer_corporate_number"))
                .values(
                        orderId, userId,
                        order.charge().subtotal().value(),
                        order.charge().discount().value(),
                        order.charge().total().value(),
                        ordererType, email, name, companyName, corporateNumber)
                .execute();

        for (Object lineObj : order.lines()) {
            OrderLine line = (OrderLine) lineObj;
            dsl.insertInto(table("order_line"))
                    .columns(
                            field("order_line_id"), field("order_id"),
                            field("product_id"), field("quantity"), field("unit_price"))
                    .values(
                            UUID.randomUUID(), orderId,
                            UUID.fromString(line.productId().value()),
                            (int) line.quantity().value(),
                            line.unitPrice().value())
                    .execute();
        }

        return OrderPlaced.decoder().decode(
                Map.of("order", Order.encoder().encode(order)), Path.ROOT).getOrThrow();
    }
}
