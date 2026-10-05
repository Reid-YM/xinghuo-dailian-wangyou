ALTER TABLE services ADD COLUMN game_code VARCHAR(32) NULL AFTER slug;
UPDATE services SET game_code = 'CODMW4' WHERE game_code IS NULL OR game_code = '';
ALTER TABLE services MODIFY COLUMN game_code VARCHAR(32) NOT NULL;
CREATE INDEX idx_services_game_category_active ON services(game_code, category, is_active, sort_order);
