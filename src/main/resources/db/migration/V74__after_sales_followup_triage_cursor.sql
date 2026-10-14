-- Staff handling cursor for customer follow-ups. Existing V71 rows default to zero.
-- The older application keeps working during candidate and canary deployment.
ALTER TABLE customer_order_after_sales_case_state
    ADD COLUMN handled_through_followup_id BIGINT NOT NULL DEFAULT 0;
