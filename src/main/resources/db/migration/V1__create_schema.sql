CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    icon VARCHAR(10) NOT NULL,
    sort_order INT,
    CONSTRAINT uk_category_name UNIQUE (name)
);

CREATE TABLE shop_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(30) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    role VARCHAR(10) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_shop_user_username UNIQUE (username)
);

CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    subtitle VARCHAR(120) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    sales INT NOT NULL,
    icon VARCHAR(10) NOT NULL,
    theme VARCHAR(20) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(15) NOT NULL,
    version BIGINT,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_cart_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES shop_user(id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(10) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    min_amount DECIMAL(10,2) NOT NULL,
    start_at TIMESTAMP(6) NOT NULL,
    end_at TIMESTAMP(6) NOT NULL,
    total_quantity INT NOT NULL,
    used_quantity INT NOT NULL,
    enabled BOOLEAN NOT NULL,
    CONSTRAINT uk_coupon_code UNIQUE (code)
);

CREATE TABLE shop_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    pay_amount DECIMAL(12,2) NOT NULL,
    coupon_code VARCHAR(30),
    status VARCHAR(20) NOT NULL,
    receiver VARCHAR(30) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(200) NOT NULL,
    remark VARCHAR(200),
    idempotency_key VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    paid_at TIMESTAMP(6),
    shipped_at TIMESTAMP(6),
    CONSTRAINT uk_order_no UNIQUE (order_no),
    CONSTRAINT uk_order_user_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES shop_user(id)
);

CREATE TABLE order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(80) NOT NULL,
    product_icon VARCHAR(10) NOT NULL,
    product_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES shop_order(id)
);
