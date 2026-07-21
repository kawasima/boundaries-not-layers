package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.Order;
import com.example.cart.domain.OrderLine;
import com.example.cart.domain.OrderRepository;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class JooqOrderRepository implements OrderRepository {

    private final DSLContext dsl;

    public JooqOrderRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public void save(Order order) {
        dsl.insertInto(table("orders"))
                .columns(
                        field("order_id"),
                        field("user_id"),
                        field("subtotal"),
                        field("discount"),
                        field("total"),
                        field("orderer_type"),
                        field("orderer_email"),
                        field("orderer_name"),
                        field("orderer_company_name"),
                        field("orderer_corporate_number"))
                .values(
                        order.orderId(),
                        order.userId(),
                        order.subtotal().amount(),
                        order.discount().amount(),
                        order.total().amount(),
                        order.orderer().type().name(),
                        order.orderer().email(),
                        order.orderer().name(),
                        order.orderer().companyName(),
                        order.orderer().corporateNumber())
                .execute();

        for (OrderLine line : order.lines()) {
            dsl.insertInto(table("order_line"))
                    .columns(
                            field("order_line_id"),
                            field("order_id"),
                            field("product_id"),
                            field("quantity"),
                            field("unit_price"))
                    .values(
                            UUID.randomUUID(),
                            order.orderId(),
                            line.productId(),
                            line.quantity(),
                            line.unitPrice().amount())
                    .execute();
        }
    }
}
