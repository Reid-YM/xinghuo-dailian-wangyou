-- Additive staff handling state for V70 after-sales records. Customer submissions remain unchanged.
CREATE TABLE customer_order_after_sales_case_state (
    record_id BIGINT NOT NULL PRIMARY KEY,
    status VARCHAR(24) NOT NULL,
    handling_note VARCHAR(600) NOT NULL,
    handled_by_admin_user_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_after_sales_case_record FOREIGN KEY (record_id)
        REFERENCES customer_order_after_sales_records (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
