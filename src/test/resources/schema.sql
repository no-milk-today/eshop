CREATE TABLE IF NOT EXISTS account(
    id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(255)   NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 10000.0,
    CONSTRAINT account_balance_non_negative CHECK (balance >= 0)
);

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