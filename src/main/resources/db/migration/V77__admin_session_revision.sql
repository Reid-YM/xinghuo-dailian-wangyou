-- Additive and backward compatible: existing passwords and enabled states are unchanged.
ALTER TABLE admin_users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;
