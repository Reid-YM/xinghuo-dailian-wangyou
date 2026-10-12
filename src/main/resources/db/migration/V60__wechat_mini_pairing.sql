-- 网站与小程序的短期登录确认。新增表仅供新版本使用，旧服务可继续运行。
CREATE TABLE wechat_mini_pairing (
    id VARCHAR(43) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    browser_hash VARCHAR(64) NOT NULL,
    target_user_id BIGINT NULL,
    mini_open_id VARCHAR(128) NULL,
    mini_union_id VARCHAR(128) NULL,
    approved_user_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    approved_at DATETIME(6) NULL,
    consumed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wechat_mini_pair_code (code_hash),
    UNIQUE KEY uk_wechat_mini_pair_browser (browser_hash),
    KEY idx_wechat_mini_pair_expires (expires_at),
    CONSTRAINT fk_wechat_mini_pair_target FOREIGN KEY (target_user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_wechat_mini_pair_approved FOREIGN KEY (approved_user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网站与小程序一次性登录确认';
