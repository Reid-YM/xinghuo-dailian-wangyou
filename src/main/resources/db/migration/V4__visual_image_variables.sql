ALTER TABLE site_config
  ADD COLUMN hero_image_height INT NOT NULL DEFAULT 150 AFTER hero_image,
  ADD COLUMN hero_image_fit VARCHAR(16) NOT NULL DEFAULT 'COVER' AFTER hero_image_height,
  ADD COLUMN hero_image_position_x INT NOT NULL DEFAULT 50 AFTER hero_image_fit,
  ADD COLUMN hero_image_position_y INT NOT NULL DEFAULT 50 AFTER hero_image_position_x,
  ADD COLUMN hero_background_image VARCHAR(500) NULL AFTER hero_image_position_y,
  ADD COLUMN hero_background_opacity INT NOT NULL DEFAULT 28 AFTER hero_background_image,
  ADD COLUMN hero_background_fit VARCHAR(16) NOT NULL DEFAULT 'COVER' AFTER hero_background_opacity,
  ADD COLUMN hero_background_position_x INT NOT NULL DEFAULT 50 AFTER hero_background_fit,
  ADD COLUMN hero_background_position_y INT NOT NULL DEFAULT 50 AFTER hero_background_position_x,
  ADD COLUMN hero_background_blur INT NOT NULL DEFAULT 0 AFTER hero_background_position_y,
  ADD COLUMN service_cover_height INT NOT NULL DEFAULT 105 AFTER hero_background_blur,
  ADD COLUMN service_cover_fit VARCHAR(16) NOT NULL DEFAULT 'COVER' AFTER service_cover_height,
  ADD COLUMN service_cover_position_x INT NOT NULL DEFAULT 50 AFTER service_cover_fit,
  ADD COLUMN service_cover_position_y INT NOT NULL DEFAULT 50 AFTER service_cover_position_x,
  ADD COLUMN wechat_qr_size INT NOT NULL DEFAULT 260 AFTER wechat_qr_image;

UPDATE site_config
SET hero_image_height = LEAST(420, GREATEST(80, hero_image_height)),
    hero_image_fit = CASE WHEN UPPER(hero_image_fit) IN ('COVER','CONTAIN','FILL') THEN UPPER(hero_image_fit) ELSE 'COVER' END,
    hero_image_position_x = LEAST(100, GREATEST(0, hero_image_position_x)),
    hero_image_position_y = LEAST(100, GREATEST(0, hero_image_position_y)),
    hero_background_opacity = LEAST(100, GREATEST(0, hero_background_opacity)),
    hero_background_fit = CASE WHEN UPPER(hero_background_fit) IN ('COVER','CONTAIN','FILL') THEN UPPER(hero_background_fit) ELSE 'COVER' END,
    hero_background_position_x = LEAST(100, GREATEST(0, hero_background_position_x)),
    hero_background_position_y = LEAST(100, GREATEST(0, hero_background_position_y)),
    hero_background_blur = LEAST(24, GREATEST(0, hero_background_blur)),
    service_cover_height = LEAST(240, GREATEST(70, service_cover_height)),
    service_cover_fit = CASE WHEN UPPER(service_cover_fit) IN ('COVER','CONTAIN','FILL') THEN UPPER(service_cover_fit) ELSE 'COVER' END,
    service_cover_position_x = LEAST(100, GREATEST(0, service_cover_position_x)),
    service_cover_position_y = LEAST(100, GREATEST(0, service_cover_position_y)),
    wechat_qr_size = LEAST(420, GREATEST(120, wechat_qr_size))
WHERE id = 1;
