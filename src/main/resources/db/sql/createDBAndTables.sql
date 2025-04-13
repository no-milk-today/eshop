CREATE DATABASE IF NOT EXISTS reshop;

-- Schema for the `users` table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

-- Schema for the `products` table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DOUBLE NOT NULL,
    description VARCHAR(500),
    img_path VARCHAR(255)
);


-- Schema for the `orders` table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_date TIMESTAMP NOT NULL,
    number VARCHAR(255),
    total_sum DOUBLE,
    CONSTRAINT fk_orders_users FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Schema for m2m relationship between orders and products
CREATE TABLE IF NOT EXISTS order_products (
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (order_id, product_id),
    CONSTRAINT fk_order_products_orders FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_products_products FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Schema for the `carts` table
CREATE TABLE IF NOT EXISTS carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_price DOUBLE NOT NULL,
    CONSTRAINT fk_carts_users FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Schema for m2m relationship between carts and products
CREATE TABLE IF NOT EXISTS cart_products (
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (cart_id, product_id),
    CONSTRAINT fk_cart_products_carts FOREIGN KEY (cart_id) REFERENCES carts(id),
    CONSTRAINT fk_cart_products_products FOREIGN KEY (product_id) REFERENCES products(id)
);

