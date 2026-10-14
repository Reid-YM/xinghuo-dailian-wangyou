-- Additive ledger for new customer after-sales submissions. Historical note blocks remain readable.
CREATE TABLE customer_order_after_sales_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(40) NOT NULL,
    category VARCHAR(24) NOT NULL,
    category_label VARCHAR(32) NOT NULL,
    content VARCHAR(240) NOT NULL,
    submitted_at_ms BIGINT NOT NULL,
    INDEX idx_after_sales_order_time (order_no, submitted_at_ms, id),
    CONSTRAINT fk_after_sales_order FOREIGN KEY (order_no)
        REFERENCES customer_orders (order_no) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
