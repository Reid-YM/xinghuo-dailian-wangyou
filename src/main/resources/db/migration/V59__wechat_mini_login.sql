-- 微信小程序扫码确认网站登录：只新增独立会话表，不改动既有账号/订单表。
CREATE TABLE wechat_qr_login (
    id                  VARCHAR(32)   NOT NULL COMMENT '小程序码 scene，一次性随机标识',
    browser_secret_hash CHAR(64)      NOT NULL COMMENT '浏览器绑定 secret 的 SHA-256，仅服务端保存哈希',
    status              VARCHAR(16)   NOT NULL COMMENT 'PENDING / CONFIRMED / CONSUMED / EXPIRED',
    user_id             BIGINT        NULL COMMENT '小程序确认后绑定的星火账号',
    return_path         VARCHAR(1000) NOT NULL DEFAULT '/account',
    created_at          DATETIME(6)   NOT NULL,
    expires_at          DATETIME(6)   NOT NULL,
    confirmed_at        DATETIME(6)   NULL,
    consumed_at         DATETIME(6)   NULL,
    version             INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_wechat_qr_status_expire (status, expires_at),
    KEY idx_wechat_qr_user (user_id),
    CONSTRAINT fk_wechat_qr_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信小程序扫码登录一次性会话';

-- union_id 用于把同一微信开放平台主体下的网站应用与小程序身份归并到同一账号。
CREATE INDEX idx_identity_provider_union ON user_identity (provider, union_id);
