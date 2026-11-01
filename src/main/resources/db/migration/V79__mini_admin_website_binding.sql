-- 星火小程序管理员绑定：MySQL 8 / InnoDB；仅新增表，无生产数据改写。
-- 微信 OpenID 和八位短码只保存 SHA-256 摘要。
CREATE TABLE xw_mini_admin_challenges (
  code_hash CHAR(64) NOT NULL PRIMARY KEY,
  mini_openid_hash CHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  consumed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_xw_mini_admin_challenges_expiry (expires_at),
  KEY idx_xw_mini_admin_challenges_openid (mini_openid_hash, consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序管理员绑定短码，不保存明文';

CREATE TABLE xw_mini_admin_bindings (
  admin_user_id BIGINT NOT NULL PRIMARY KEY,
  mini_openid_hash CHAR(64) NOT NULL,
  admin_auth_version BIGINT NOT NULL,
  bound_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_xw_mini_admin_binding_openid (mini_openid_hash),
  CONSTRAINT fk_xw_mini_admin_binding_user FOREIGN KEY (admin_user_id)
    REFERENCES admin_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网站管理员与小程序微信身份显式绑定';
