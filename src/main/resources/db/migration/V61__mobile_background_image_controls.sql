ALTER TABLE site_config
    ADD COLUMN hero_background_mobile_image VARCHAR(500) NULL AFTER hero_background_image,
    ADD COLUMN hero_background_mobile_height INT NOT NULL DEFAULT 520 AFTER hero_background_mobile_image;
