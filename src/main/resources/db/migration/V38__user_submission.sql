-- =============================================================================
-- V38 用户系统 · 攻略投稿与审核
--
-- 与现有 game_guides 的关系：审核通过后由后台把内容发布进 game_guides，
-- 这里只保留投稿本身的痕迹与发布结果（published_guide_slug），
-- 不复制攻略正文结构 —— 否则同一篇内容会有两份真相。
-- =============================================================================

CREATE TABLE user_submission (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NOT NULL,
    game_code          VARCHAR(32)  NOT NULL,
    title              VARCHAR(180) NOT NULL,
    summary            VARCHAR(500) NULL,
    content_md         MEDIUMTEXT   NOT NULL COMMENT 'Markdown 原文，渲染时走标签白名单',
    cover_image        VARCHAR(500) NULL,
    status             VARCHAR(16)  NOT NULL DEFAULT 'SUBMITTED'
                       COMMENT 'SUBMITTED / IN_REVIEW / APPROVED / REJECTED / PUBLISHED / WITHDRAWN',
    reviewer_name      VARCHAR(80)  NULL COMMENT '审核人用户名',
    review_note        VARCHAR(500) NULL,
    published_slug     VARCHAR(140) NULL COMMENT '发布后对应的 game_guides.slug',
    published_at       DATETIME(6)  NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_submission_status_time (status, created_at),
    KEY idx_submission_user_time (user_id, created_at),
    KEY idx_submission_game (game_code, status),
    CONSTRAINT fk_submission_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户攻略投稿';
