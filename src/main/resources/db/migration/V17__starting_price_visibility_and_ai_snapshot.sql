ALTER TABLE site_config
  ADD COLUMN show_starting_price BOOLEAN NOT NULL DEFAULT TRUE AFTER starting_price;

UPDATE site_config
SET show_starting_price = TRUE
WHERE id = 1;
