-- Nullable and additive: old application versions ignore this column.
ALTER TABLE xw_admin_profiles
    ADD COLUMN wecom_user_id VARCHAR(64) NULL;

CREATE UNIQUE INDEX uk_xw_admin_profile_wecom_user_id
    ON xw_admin_profiles (wecom_user_id);
