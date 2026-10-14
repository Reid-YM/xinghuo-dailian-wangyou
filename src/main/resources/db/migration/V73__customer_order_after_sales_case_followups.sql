-- Customer-authored follow-ups on an existing V70 after-sales record.
-- Additive and readable independently of the V71 state and V72 staff events.
CREATE TABLE customer_order_after_sales_case_followups (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    content VARCHAR(240) NOT NULL,
    created_at_ms BIGINT NOT NULL,
    INDEX idx_after_sales_followup_record (record_id, id),
    CONSTRAINT fk_after_sales_followup_record FOREIGN KEY (record_id)
        REFERENCES customer_order_after_sales_records (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
