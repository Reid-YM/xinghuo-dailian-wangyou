-- Mini admin access is always confirmed inside the authenticated website admin session.
-- Store salted-independent SHA-256 digests only; never persist a wx.login code or browser cookie.
CREATE TABLE xw_mini_admin_challenges (
  code_hash CHAR(64) PRIMARY KEY,
  mini_openid_hash CHAR(64) NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  consumed_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_xw_mini_admin_challenges_expiry ON xw_mini_admin_challenges(expires_at);

CREATE TABLE xw_mini_admin_bindings (
  admin_user_id BIGINT PRIMARY KEY REFERENCES admin_users(id) ON DELETE CASCADE,
  mini_openid_hash CHAR(64) NOT NULL UNIQUE,
  admin_auth_version BIGINT NOT NULL,
  bound_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);