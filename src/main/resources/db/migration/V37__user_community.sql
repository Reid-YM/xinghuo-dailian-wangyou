-- =============================================================================
-- V37 用户系统 · 社区互动（评论与收藏）
--
-- target_type + target_id 采用「弱关联」而不是外键：
-- 评论与收藏可以指向 game_guides.slug、game_catalog.code 或 services.id，
-- 三类主键类型都不同（字符串 / 字符串 / 自增数字），无法用统一外键约束。
-- 代价是被指向内容删除后会留下孤儿行，由后台清理任务按 target_type 分别处理。
-- =============================================================================

CREATE TABLE user_comment (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL,
    target_type VARCHAR(16)   NOT NULL COMMENT 'GUIDE / GAME / SERVICE',
    target_id   VARCHAR(64)   NOT NULL COMMENT '攻略 slug / 游戏 code / 服务 id',
    parent_id   BIGINT        NULL COMMENT '一级回复；不做多级嵌套',
    content     VARCHAR(2000) NOT NULL COMMENT '纯文本，渲染时转义，不接受用户 HTML',
    status      VARCHAR(16)   NOT NULL DEFAULT 'PUBLISHED'
                COMMENT 'PENDING（新用户首条待审）/ PUBLISHED / REJECTED / DELETED',
    audit_note  VARCHAR(300)  NULL COMMENT '审核意见',
    reviewed_by_name VARCHAR(80) NULL COMMENT '管理员用户名；后台登录身份是用户名而非数字 id',
    reviewed_at DATETIME(6)   NULL,
    report_count INT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_comment_target (target_type, target_id, status, created_at),
    KEY idx_comment_user_time (user_id, created_at),
    KEY idx_comment_status_time (status, created_at),
    CONSTRAINT fk_comment_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id)
        REFERENCES user_comment (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='攻略/游戏/服务评论';

CREATE TABLE user_comment_report (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    comment_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    reason     VARCHAR(120) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    -- 同一人对同一条评论只能举报一次
    UNIQUE KEY uk_report_once (comment_id, user_id),
    KEY idx_report_comment (comment_id),
    CONSTRAINT fk_report_comment FOREIGN KEY (comment_id)
        REFERENCES user_comment (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论举报';

CREATE TABLE user_favorite (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    target_type VARCHAR(16) NOT NULL COMMENT 'GUIDE / GAME / SERVICE',
    target_id   VARCHAR(64) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite (user_id, target_type, target_id),
    KEY idx_favorite_user_time (user_id, created_at),
    CONSTRAINT fk_favorite_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏';
