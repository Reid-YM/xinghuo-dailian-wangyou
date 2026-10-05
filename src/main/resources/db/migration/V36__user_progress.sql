-- =============================================================================
-- V36 用户系统 · 攻略进度云同步
--
-- 关于订单归属的说明：本迁移**不建 user_order_link 表**。
-- 原方案设计的独立关联表是多余的：customer_orders.customer_account_id 与
-- quotes.customer_account_id 由 V33 建立、V34 的 marketplace 也在使用，
-- 已经是订单归属的唯一真相来源。再建一张表会产生双重来源，
-- 后续任何一处更新遗漏都会导致归属错乱。
-- 认领动作的来源与时间改由 user_audit_log 记录（action = ORDER_CLAIM）。
-- =============================================================================

-- 攻略工具进度：mastery / camo / 配装 等按 tool_key 存一份 JSON
CREATE TABLE user_progress (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    tool_key   VARCHAR(40) NOT NULL COMMENT '工具标识，如 mastery / camo / weapon',
    -- 刻意用 MEDIUMTEXT 而非 JSON 列：
    -- 本项目的实体映射没有 JSON 列的先例，而 spring.jpa.hibernate.ddl-auto=validate
    -- 会在启动时校验列类型，映射不当会直接导致服务起不来。
    -- 载荷的 JSON 合法性由应用层在写入前校验，效果等同。
    payload    MEDIUMTEXT  NOT NULL,
    version    INT         NOT NULL DEFAULT 1 COMMENT '乐观锁版本，前端带旧版本提交返回 409',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_progress_user_tool (user_id, tool_key),
    CONSTRAINT fk_progress_account FOREIGN KEY (user_id)
        REFERENCES customer_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='攻略进度云同步';

-- 认领候选需要按「未归属订单」筛选，补一个索引避免全表扫描
ALTER TABLE quotes ADD INDEX idx_quotes_unclaimed (customer_account_id, created_at);
