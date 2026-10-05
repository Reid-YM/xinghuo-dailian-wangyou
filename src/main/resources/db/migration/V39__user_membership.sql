-- =============================================================================
-- V39 用户系统 · 会员权益模型
--
-- 阶段说明：本迁移只建立权益模型，不接入支付。
-- 大陆收款需要商户资质（微信支付/支付宝需营业执照），因此一期用
-- 「管理员手动开通 + 兑换码」把权益模型跑通，支付作为独立立项。
--
-- entitlements 用 MEDIUMTEXT 存 JSON 而不是 JSON 列：本模块的实体映射
-- 没有 JSON 列先例，ddl-auto=validate 下映射不当会直接导致服务起不来。
-- 该字段只由服务端读取并解析，不参与任何 SQL 层查询。
-- =============================================================================

CREATE TABLE membership_plan (
    code          VARCHAR(32)  NOT NULL,
    name          VARCHAR(60)  NOT NULL,
    description   VARCHAR(300) NULL,
    duration_days INT          NOT NULL COMMENT '0 表示永久',
    price_cents   INT          NOT NULL DEFAULT 0 COMMENT '仅供展示；一期不涉及实际收款',
    entitlements  MEDIUMTEXT   NOT NULL COMMENT 'JSON 数组，如 ["GUIDE_PRO","PROGRESS_QUOTA"]',
    sort_order    INT          NOT NULL DEFAULT 100,
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (code),
    KEY idx_plan_active_sort (active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员套餐';

CREATE TABLE membership_grant (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    plan_code  VARCHAR(32) NOT NULL,
    source     VARCHAR(16) NOT NULL COMMENT 'MANUAL（管理员开通）/ REDEEM（兑换码）/ PAYMENT（预留）',
    source_ref VARCHAR(80) NULL COMMENT '兑换码或支付单号',
    granted_by_name VARCHAR(80) NULL COMMENT '开通人用户名',
    start_at   DATETIME(6) NOT NULL,
    end_at     DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    note       VARCHAR(300) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_grant_user_active (user_id, revoked_at, end_at),
    KEY idx_grant_plan (plan_code),
    CONSTRAINT fk_grant_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权益发放记录';

-- 初始套餐：免费档用于承载默认权益，避免「没有任何 grant」与「有免费权益」
-- 两种状态在代码里分叉。付费档先建好占位，price 仅作展示。
INSERT INTO membership_plan
    (code, name, description, duration_days, price_cents, entitlements, sort_order, active, created_at, updated_at)
VALUES
    ('FREE', '普通用户', '注册即享有的基础权益', 0, 0,
     '["PROGRESS_QUOTA_BASIC"]', 10, 1, NOW(6), NOW(6)),
    ('PRO', '进阶会员', '解锁进阶攻略与更大的云同步额度', 30, 0,
     '["GUIDE_PRO","PROGRESS_QUOTA","ADVANCED_TOOLS","SUBMIT_PRIORITY"]', 20, 1, NOW(6), NOW(6));
