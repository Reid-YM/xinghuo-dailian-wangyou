-- =============================================================================
-- V42 用户系统 · 邮箱验证码、投递审计与站内通知
--
-- 安全边界：
--   * 验证码只保存 BCrypt 哈希，明文只在发送时短暂存在内存中；
--   * 邮件日志不保存正文、验证码或完整收件地址；
--   * 前端只读取站内通知，不暴露任何内部投递字段。
-- =============================================================================

CREATE TABLE email_verification_challenge (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NULL,
    purpose          VARCHAR(32)  NOT NULL COMMENT 'REGISTER / CHANGE_EMAIL_OLD / CHANGE_EMAIL_NEW',
    correlation_id   VARCHAR(48)  NOT NULL,
    target_email     VARCHAR(160) NOT NULL,
    target_email_norm VARCHAR(160) NOT NULL,
    code_hash        VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希，禁止保存明文验证码',
    attempts         INT          NOT NULL DEFAULT 0,
    expires_at       DATETIME(6)  NOT NULL,
    consumed_at      DATETIME(6)  NULL,
    request_ip       VARCHAR(45)  NULL,
    created_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_email_challenge_lookup
        (purpose, target_email_norm, user_id, consumed_at, created_at),
    KEY idx_email_challenge_correlation (correlation_id, purpose, consumed_at),
    CONSTRAINT fk_email_challenge_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一次性邮箱验证码（只存哈希）';

CREATE TABLE mail_delivery_log (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NULL,
    template_code    VARCHAR(40)  NOT NULL,
    recipient_masked VARCHAR(190) NOT NULL,
    recipient_hash   VARCHAR(64)  NOT NULL,
    status           VARCHAR(16)  NOT NULL COMMENT 'PENDING / SENT / FAILED',
    attempts         INT          NOT NULL DEFAULT 0,
    last_error_code  VARCHAR(64)  NULL,
    created_at       DATETIME(6)  NOT NULL,
    sent_at          DATETIME(6)  NULL,
    PRIMARY KEY (id),
    KEY idx_mail_status_time (status, created_at),
    KEY idx_mail_user_time (user_id, created_at),
    KEY idx_mail_recipient_time (recipient_hash, created_at),
    CONSTRAINT fk_mail_log_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件投递审计（不保存正文与完整地址）';

CREATE TABLE user_notification (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    category   VARCHAR(24)  NOT NULL COMMENT 'ACCOUNT / SECURITY / ORDER / SYSTEM',
    title      VARCHAR(120) NOT NULL,
    content    VARCHAR(500) NOT NULL,
    action_url VARCHAR(300) NULL,
    read_at    DATETIME(6)  NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notification_user_time (user_id, created_at),
    KEY idx_notification_unread (user_id, read_at, created_at),
    CONSTRAINT fk_notification_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户站内通知';

CREATE TABLE user_risk_flag (
    user_id         BIGINT       NOT NULL,
    risk_level      VARCHAR(16)  NOT NULL COMMENT 'NONE / WATCH / HIGH',
    note            VARCHAR(300) NULL,
    updated_by_name VARCHAR(80)  NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_id),
    KEY idx_risk_level (risk_level, updated_at),
    CONSTRAINT fk_risk_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台风险用户标记';

CREATE TABLE mail_template (
    code             VARCHAR(40)  NOT NULL,
    subject_template VARCHAR(200) NOT NULL,
    body_template    MEDIUMTEXT   NOT NULL COMMENT '使用 {content} 插入服务端生成的安全正文',
    active           TINYINT(1)   NOT NULL DEFAULT 1,
    updated_by_name  VARCHAR(80)  NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件主题与正文包装模板';

INSERT INTO mail_template (code, subject_template, body_template, active, updated_at) VALUES
    ('REGISTER_CODE', '星火网游 · 注册验证码', '{content}', 1, NOW(6)),
    ('WELCOME', '欢迎加入星火网游', '{content}', 1, NOW(6)),
    ('VERIFY_EMAIL', '星火网游 · 邮箱验证', '{content}', 1, NOW(6)),
    ('RESET_PASSWORD', '星火网游 · 重置密码', '{content}', 1, NOW(6)),
    ('CHANGE_EMAIL_OLD', '星火网游 · 确认更换安全邮箱', '{content}', 1, NOW(6)),
    ('CHANGE_EMAIL_NEW', '星火网游 · 验证新安全邮箱', '{content}', 1, NOW(6)),
    ('NEW_DEVICE', '星火网游 · 新设备登录提醒', '{content}', 1, NOW(6));
