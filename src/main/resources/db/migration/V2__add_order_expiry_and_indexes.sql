ALTER TABLE shop_order ADD COLUMN expires_at TIMESTAMP(6);

CREATE INDEX idx_shop_order_user_created ON shop_order(user_id, created_at);
CREATE INDEX idx_shop_order_status_expires ON shop_order(status, expires_at);
CREATE INDEX idx_shop_order_status_created ON shop_order(status, created_at);
CREATE INDEX idx_product_category_status ON product(category_id, status);
CREATE INDEX idx_product_status_sales ON product(status, sales);
CREATE INDEX idx_cart_item_user_updated ON cart_item(user_id, updated_at);
CREATE INDEX idx_coupon_active ON coupon(enabled, end_at);
