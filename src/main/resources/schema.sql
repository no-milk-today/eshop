CREATE TABLE IF NOT EXISTS account
(
    id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(255)   NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 10000.0,
    CONSTRAINT account_balance_non_negative CHECK (balance >= 0)
);