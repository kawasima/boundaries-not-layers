package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.Order;
import com.example.cart.domain.OrderLine;
import java.util.Map;
import java.util.UUID;
import net.unit8.raoh.encode.Encoder;
import org.jooq.DSLContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/**
 * 注文永続化の gateway。ヘッダ1行と明細 N 行を書く。行の値は {@link JooqOrderEncoders} が組む。
 */
@Repository
public class JooqOrderRepository implements com.example.cart.domain.OrderRepository {

    private final DSLContext dsl;

    public JooqOrderRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public void save(Order order) {
        Map<String, @Nullable Object> header = JooqOrderEncoders.ORDER_ROW.encode(order);
        // 注文者（sealed）を判別エンコードでフラットな列に。個人/法人で埋まる列が変わる（他方は null）。
        Map<String, @Nullable Object> orderer = JooqOrderEncoders.ORDERER_ROW.encode(order.orderer());
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
                        order.id().value(),
                        header.get("user_id"),
                        header.get("subtotal"),
                        header.get("discount"),
                        header.get("total"),
                        orderer.get("orderer_type"),
                        orderer.get("orderer_email"),
                        orderer.get("orderer_name"),
                        orderer.get("orderer_company_name"),
                        orderer.get("orderer_corporate_number"))
                .execute();

        Encoder<OrderLine, Map<String, @Nullable Object>> lineEncoder = JooqOrderEncoders.ORDER_LINE_ROW;
        for (OrderLine line : order.lines()) {
            Map<String, @Nullable Object> row = lineEncoder.encode(line);
            dsl.insertInto(table("order_line"))
                    .columns(
                            field("order_line_id"),
                            field("order_id"),
                            field("product_id"),
                            field("quantity"),
                            field("unit_price"))
                    .values(
                            UUID.randomUUID(),
                            order.id().value(),
                            row.get("product_id"),
                            row.get("quantity"),
                            row.get("unit_price"))
                    .execute();
        }
    }
}
