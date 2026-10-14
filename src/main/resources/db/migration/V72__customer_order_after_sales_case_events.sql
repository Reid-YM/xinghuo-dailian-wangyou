-- Additive history for staff transitions and explicit customer-visible replies.
-- Existing V71 state remains authoritative; older actions retain their audit entries.
CREATE TABLE customer_order_after_sales_case_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    from_status VARCHAR(24) NOT NULL,
    to_status VARCHAR(24) NOT NULL,
    handling_note VARCHAR(600) NOT NULL,
    customer_reply VARCHAR(600) NULL,
    handled_by_admin_user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_after_sales_case_event_record (record_id, id),
    CONSTRAINT fk_after_sales_case_event_record FOREIGN KEY (record_id)
        REFERENCES customer_order_after_sales_records (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
