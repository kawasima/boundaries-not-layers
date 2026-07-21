CREATE TABLE IF NOT EXISTS product (
    product_id UUID PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    on_sale    BOOLEAN      NOT NULL,
    price      BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS cart (
    cart_id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS cart_item (
    cart_item_id UUID PRIMARY KEY,
    cart_id      UUID NOT NULL,
    product_id   UUID NOT NULL,
    quantity     INT  NOT NULL,
    CONSTRAINT uq_cart_product UNIQUE (cart_id, product_id)
);

CREATE TABLE IF NOT EXISTS orders (
    order_id                 UUID PRIMARY KEY,
    user_id                  UUID   NOT NULL,
    subtotal                 BIGINT NOT NULL,
    discount                 BIGINT NOT NULL,
    total                    BIGINT NOT NULL,
    orderer_type             VARCHAR(20)  NOT NULL,
    orderer_email            VARCHAR(200) NOT NULL,
    orderer_name             VARCHAR(100),
    orderer_company_name     VARCHAR(200),
    orderer_corporate_number VARCHAR(13)
);

CREATE TABLE IF NOT EXISTS order_line (
    order_line_id UUID PRIMARY KEY,
    order_id      UUID   NOT NULL,
    product_id    UUID   NOT NULL,
    quantity      INT    NOT NULL,
    unit_price    BIGINT NOT NULL
);
