ALTER TABLE site_config
  ADD COLUMN legal_entity_name VARCHAR(120) NOT NULL DEFAULT '星维网络服务工作室' AFTER brand_name;

UPDATE site_config
SET legal_entity_name = '星维网络服务工作室'
WHERE legal_entity_name IS NULL OR legal_entity_name = '';
