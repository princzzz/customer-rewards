CREATE TABLE customers (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE purchase_transactions (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    purchased_on DATE NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_purchase_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT chk_purchase_amount CHECK (amount >= 0)
);

CREATE INDEX idx_purchase_date_customer ON purchase_transactions (purchased_on, customer_id);