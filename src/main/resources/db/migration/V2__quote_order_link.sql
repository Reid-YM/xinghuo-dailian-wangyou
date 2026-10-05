ALTER TABLE customer_orders
  ADD COLUMN source_quote_id CHAR(36) NULL AFTER order_no,
  ADD UNIQUE INDEX uq_orders_source_quote (source_quote_id),
  ADD CONSTRAINT fk_order_source_quote FOREIGN KEY (source_quote_id) REFERENCES quotes(id) ON DELETE SET NULL;
