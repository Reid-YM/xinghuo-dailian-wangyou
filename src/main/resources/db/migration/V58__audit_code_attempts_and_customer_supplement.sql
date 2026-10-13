-- Additive changes: old and new application instances can run concurrently.
ALTER TABLE user_token ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0;
-- Existing mixed internal notes stay private; never copy them into the customer-visible field.
ALTER TABLE quotes ADD COLUMN customer_supplement TEXT NULL;
