ALTER TABLE site_config
  ADD COLUMN hero_image_mobile_height INT NOT NULL DEFAULT 150 AFTER hero_image_position_y,
  ADD COLUMN hero_image_mobile_fit VARCHAR(16) NOT NULL DEFAULT 'COVER' AFTER hero_image_mobile_height,
  ADD COLUMN hero_image_mobile_position_x INT NOT NULL DEFAULT 50 AFTER hero_image_mobile_fit,
  ADD COLUMN hero_image_mobile_position_y INT NOT NULL DEFAULT 50 AFTER hero_image_mobile_position_x,
  ADD COLUMN hero_background_mobile_fit VARCHAR(16) NOT NULL DEFAULT 'COVER' AFTER hero_background_blur,
  ADD COLUMN hero_background_mobile_position_x INT NOT NULL DEFAULT 50 AFTER hero_background_mobile_fit,
  ADD COLUMN hero_background_mobile_position_y INT NOT NULL DEFAULT 50 AFTER hero_background_mobile_position_x,
  ADD COLUMN service_cover_mobile_height INT NOT NULL DEFAULT 105 AFTER service_cover_position_y,
  ADD COLUMN service_cover_mobile_fit VARCHAR(16) NOT NULL DEFAULT 'COVER' AFTER service_cover_mobile_height,
  ADD COLUMN service_cover_mobile_position_x INT NOT NULL DEFAULT 50 AFTER service_cover_mobile_fit,
  ADD COLUMN service_cover_mobile_position_y INT NOT NULL DEFAULT 50 AFTER service_cover_mobile_position_x;

UPDATE site_config
SET hero_image_mobile_height = LEAST(420, GREATEST(80, hero_image_height)),
    hero_image_mobile_fit = CASE WHEN UPPER(hero_image_fit) IN ('COVER','CONTAIN','FILL') THEN UPPER(hero_image_fit) ELSE 'COVER' END,
    hero_image_mobile_position_x = LEAST(100, GREATEST(0, hero_image_position_x)),
    hero_image_mobile_position_y = LEAST(100, GREATEST(0, hero_image_position_y)),
    hero_background_mobile_fit = CASE WHEN UPPER(hero_background_fit) IN ('COVER','CONTAIN','FILL') THEN UPPER(hero_background_fit) ELSE 'COVER' END,
    hero_background_mobile_position_x = LEAST(100, GREATEST(0, hero_background_position_x)),
    hero_background_mobile_position_y = LEAST(100, GREATEST(0, hero_background_position_y)),
    service_cover_mobile_height = LEAST(240, GREATEST(70, service_cover_height)),
    service_cover_mobile_fit = CASE WHEN UPPER(service_cover_fit) IN ('COVER','CONTAIN','FILL') THEN UPPER(service_cover_fit) ELSE 'COVER' END,
    service_cover_mobile_position_x = LEAST(100, GREATEST(0, service_cover_position_x)),
    service_cover_mobile_position_y = LEAST(100, GREATEST(0, service_cover_position_y));
