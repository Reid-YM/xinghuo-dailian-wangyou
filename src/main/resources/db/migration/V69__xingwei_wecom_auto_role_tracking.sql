-- Track only roles assigned from a verified WeCom position so later changes can be revoked.
-- Existing accounts and their manually assigned roles are untouched.
CREATE TABLE xw_wecom_auto_roles (
    admin_user_id BIGINT NOT NULL PRIMARY KEY,
    position_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    wecom_position VARCHAR(80) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_xw_wecom_auto_role_user FOREIGN KEY (admin_user_id)
        REFERENCES admin_users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
