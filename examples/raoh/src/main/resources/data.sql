-- デモ用のユーザー・カート・商品を投入する（再実行に備えて MERGE で冪等に）
MERGE INTO cart (cart_id, user_id) KEY (cart_id) VALUES
    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111');

-- 販売中の商品（単価 1200 円）
MERGE INTO product (product_id, name, on_sale, price) KEY (product_id) VALUES
    ('33333333-3333-3333-3333-333333333333', 'Coffee Beans 500g', TRUE, 1200);

-- 販売終了した商品（単価 800 円）
MERGE INTO product (product_id, name, on_sale, price) KEY (product_id) VALUES
    ('44444444-4444-4444-4444-444444444444', 'Discontinued Mug', FALSE, 800);
