package com.example.cart.infrastructure;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import com.example.cart.domain.ProductRepository;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class JooqProductRepository implements ProductRepository {

    private final DSLContext dsl;

    public JooqProductRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public boolean isNowOnSale(UUID productId) {
        Boolean onSale = dsl.select(field("on_sale", Boolean.class))
                .from(table("product"))
                .where(field("product_id", UUID.class).eq(productId))
                .fetchOne(field("on_sale", Boolean.class));
        return Boolean.TRUE.equals(onSale);
    }

    @Override
    public long priceOf(UUID productId) {
        Long price = dsl.select(field("price", Long.class))
                .from(table("product"))
                .where(field("product_id", UUID.class).eq(productId))
                .fetchOne(field("price", Long.class));
        return price == null ? 0 : price;
    }
}
