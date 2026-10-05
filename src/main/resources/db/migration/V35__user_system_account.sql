-- =============================================================================
-- V35 用户系统 · 账号核心（从零重建）
--
-- 背景与依据（均已在服务器上核对）：
--   * 基线迁移最大版本为 V34__marketplace_0_1_foundation.sql，故本迁移为 V35。
--   * 旧账号实现在基线 JAR 内：CustomerAccount / CustomerAccountService /
--     CustomerAccountController / CustomerAccountRepository / CustomerDtos$Customer*。
--     它们全部由本次重建取代，并已加入 scripts/delete-jar-entries.txt。
--   * 经二进制扫描确认：除 CustomerAccount 实体自身外，没有任何类读写
--     customer_accounts 的列。Quote / CustomerOrder 只持有普通 Long
--     customerAccountId 字段，不是 @ManyToOne 关联，数据库也无外键指向该表。
--     因此可以安全地重建本表。
--
-- 表名保留 customer_accounts 是刻意的：quotes.customer_account_id 与
-- customer_orders.customer_account_id、以及 V34 的 seller_profiles /
-- booster_profiles 都以它为身份锚点。沿用表名可让这些既有引用保持语义正确，
-- 无需改动 marketplace 相关结构。
--
-- 旧表不 DROP，而是重命名归档，便于事后核对与人工回滚。
--
-- ⚠️ 执行前置条件（已在干跑库上实测）
--   本迁移第 1 步的 RENAME TABLE 需要原表的 DROP 权限，而应用账号 xinghuo_v2
--   只有 SELECT/INSERT/UPDATE/DELETE/CREATE/REFERENCES/INDEX/ALTER，没有 DROP。
--   若直接由 Flyway 执行，会以
--       DROP command denied to user 'xinghuo_v2' for table 'customer_accounts'
--   失败，并把库留在「迁移已开始但未完成」的状态。
--
--   因此本项目的部署流程要求：**先用管理账号执行
--   scripts/archive-legacy-customer-accounts.sql 完成归档，再部署新版本**。
--   归档完成后本迁移的 @exists 判定为 0，会走 'DO 0' 分支跳过重命名，
--   其余建表与数据订正语句都能用应用账号的权限完成。
--
--   之所以不干脆给应用账号加 DROP：破坏性操作交给管理账号，应用进程即使被
--   SQL 注入也不具备删表能力，这是刻意的权限收敛。
-- =============================================================================

-- ---------------------------------------------------------------- 1. 归档旧表
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_accounts');
SET @stmt := IF(@exists > 0,
                'RENAME TABLE customer_accounts TO customer_accounts_legacy_20261001',
                'DO 0');
PREPARE archive_stmt FROM @stmt;
EXECUTE archive_stmt;
DEALLOCATE PREPARE archive_stmt;

-- ------------------------------------------------------------------ 2. 新账号表
-- 若上一步未成功归档（例如 legacy 表名已存在），这里的 CREATE 会直接失败报错，
-- 而不是静默复用一张结构不符的旧表。
CREATE TABLE customer_accounts (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    contact_type     VARCHAR(16)  NOT NULL COMMENT 'WECHAT / QQ / PHONE',
    contact_value    VARCHAR(120) NOT NULL COMMENT '原样保存，用于展示与客服核对',
    contact_norm     VARCHAR(120) NOT NULL COMMENT '规范化值，用于查重与历史订单认领匹配',
    contact_verified TINYINT(1)   NOT NULL DEFAULT 0,
    email            VARCHAR(160) NULL,
    email_norm       VARCHAR(160) NULL COMMENT '唯一索引允许多个 NULL',
    email_verified   TINYINT(1)   NOT NULL DEFAULT 0,
    password_hash    VARCHAR(100) NOT NULL COMMENT 'BCrypt cost=12',
    display_name     VARCHAR(60)  NOT NULL,
    avatar_url       VARCHAR(500) NULL,
    role             VARCHAR(24)  NOT NULL DEFAULT 'BUYER' COMMENT '预留：BUYER / SELLER / BOOSTER，与 marketplace 对齐',
    level_code       VARCHAR(24)  NOT NULL DEFAULT 'NEW' COMMENT '预留：会员等级，权益模型落地后使用',
    status           VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / LOCKED / DISABLED / DELETED',
    failed_attempts  INT          NOT NULL DEFAULT 0,
    locked_until     DATETIME(6)  NULL,
    last_login_at    DATETIME(6)  NULL,
    last_login_ip    VARCHAR(45)  NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    deleted_at       DATETIME(6)  NULL COMMENT '软删，冷静期后由清理任务物理删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_contact_norm (contact_type, contact_norm),
    UNIQUE KEY uk_customer_email (email_norm),
    KEY idx_customer_status (status, created_at),
    KEY idx_customer_role_level (role, level_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账号主表（V35 重建）';

-- ------------------------------------------------- 3. 切断指向旧账号的悬空引用
-- 归档后旧的 customer_account_id 已无对应账号；若不清理，新账号从 id=1 重新
-- 开始时，历史报价/订单会错误地显示成新用户的业务。置空后可由用户通过
-- 「历史订单认领」重新绑定。
UPDATE quotes          SET customer_account_id = NULL WHERE customer_account_id IS NOT NULL;
UPDATE customer_orders SET customer_account_id = NULL WHERE customer_account_id IS NOT NULL;

-- ------------------------------------------------------------ 4. 持久化会话
-- 解决蓝绿发布清空 JVM 内存 Session 导致全员掉线的问题；同时取代旧实现
-- 「登录失败计数存在 HttpSession 里、清 Cookie 即可重置」的做法。
CREATE TABLE user_session (
    id           VARCHAR(64)  NOT NULL COMMENT '32 字节 SecureRandom 的 Base64URL 编码（43 字符），写入 Cookie XH_SID',
    user_id      BIGINT       NOT NULL,
    ip           VARCHAR(45)  NULL,
    user_agent   VARCHAR(300) NULL,
    device_label VARCHAR(80)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    last_seen_at DATETIME(6)  NOT NULL,
    expires_at   DATETIME(6)  NOT NULL,
    revoked_at   DATETIME(6)  NULL,
    PRIMARY KEY (id),
    KEY idx_session_user_active (user_id, revoked_at, expires_at),
    KEY idx_session_expires (expires_at),
    CONSTRAINT fk_session_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='持久化登录会话';

-- ------------------------------------------------------- 5. 第三方身份绑定
CREATE TABLE user_identity (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    provider      VARCHAR(16)  NOT NULL COMMENT 'QQ / WECHAT',
    open_id       VARCHAR(128) NOT NULL COMMENT '按应用隔离，QQ 与微信 openid 不可互认',
    union_id      VARCHAR(128) NULL,
    nickname      VARCHAR(120) NULL,
    avatar_url    VARCHAR(500) NULL,
    created_at    DATETIME(6)  NOT NULL,
    last_login_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_identity_provider_openid (provider, open_id),
    KEY idx_identity_user (user_id),
    CONSTRAINT fk_identity_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方登录绑定';

-- --------------------------------------------------------- 6. 一次性令牌
CREATE TABLE user_token (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    purpose    VARCHAR(24)  NOT NULL COMMENT 'VERIFY_EMAIL / RESET_PASSWORD / CHANGE_EMAIL',
    token_hash VARCHAR(64)  NOT NULL COMMENT 'SHA-256 十六进制；明文只出现在邮件链接中',
    payload    VARCHAR(200) NULL,
    expires_at DATETIME(6)  NOT NULL,
    used_at    DATETIME(6)  NULL,
    request_ip VARCHAR(45)  NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_token_hash (token_hash),
    KEY idx_token_user_purpose (user_id, purpose, used_at),
    KEY idx_token_expires (expires_at),
    CONSTRAINT fk_token_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一次性令牌（只存哈希）';

-- ------------------------------------------------------------- 7. 审计日志
CREATE TABLE user_audit_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NULL,
    action     VARCHAR(40)  NOT NULL COMMENT 'REGISTER/LOGIN_OK/LOGIN_FAIL/LOGOUT/PWD_CHANGE/SESSION_REVOKE/...',
    detail     VARCHAR(500) NULL COMMENT '已脱敏，禁止写入手机号/邮箱/微信号明文',
    ip         VARCHAR(45)  NULL,
    user_agent VARCHAR(300) NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_user_time (user_id, created_at),
    KEY idx_audit_action_time (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户系统审计日志';
