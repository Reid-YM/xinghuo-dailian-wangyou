-- =============================================================================
-- V40 支付基础设施
--
-- 设计要点：
--   1. 与支付厂商无关。provider 字段区分 PAYPAL / WECHAT / ALIPAY，
--      上层业务只认 payment_order，换厂商不动订单逻辑。
--   2. 金额与币种都落库。本站服务价格是人民币，而 PayPal 收款可能需要外币，
--      因此不假设「站内价 == 收款额」，两者分开存、由服务端换算并留痕。
--   3. 三层幂等：payment_no（我方单号）、(provider, provider_order_id)、
--      以及 webhook 的 event_id 唯一键 —— 支付回调必然会重复投递。
--   4. 原始报文一律留存（payment_webhook_event.raw_body、
--      payment_transaction.raw_json）：对账与纠纷时这是唯一可信证据。
-- =============================================================================

CREATE TABLE payment_order (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    payment_no         VARCHAR(32)   NOT NULL COMMENT '我方支付单号，对外可见',
    user_id            BIGINT        NULL COMMENT '下单账号；允许未登录先付款，故可空',
    business_no        VARCHAR(40)   NULL COMMENT '关联 customer_orders.business_no',
    subject            VARCHAR(200)  NOT NULL COMMENT '商品描述，展示给付款方',
    amount             DECIMAL(12,2) NOT NULL COMMENT '实际收款金额',
    currency           VARCHAR(8)    NOT NULL COMMENT '收款币种，如 USD / CNY',
    site_amount        DECIMAL(12,2) NULL COMMENT '站内记账金额（人民币）',
    site_currency      VARCHAR(8)    NOT NULL DEFAULT 'CNY',
    exchange_rate      DECIMAL(12,6) NULL COMMENT '换算汇率，留痕便于对账',
    provider           VARCHAR(16)   NOT NULL COMMENT 'PAYPAL / WECHAT / ALIPAY',
    provider_order_id  VARCHAR(64)   NULL COMMENT '厂商订单号',
    provider_capture_id VARCHAR(64)  NULL COMMENT '厂商捕获号，退款要用',
    status             VARCHAR(20)   NOT NULL DEFAULT 'CREATED'
                       COMMENT 'CREATED / APPROVED / CAPTURED / FAILED / CANCELLED / REFUNDED / PARTIALLY_REFUNDED',
    payer_ref          VARCHAR(120)  NULL COMMENT '付款方标识，已脱敏',
    idempotency_key    VARCHAR(64)   NOT NULL COMMENT '客户端幂等键，防重复建单',
    fail_reason        VARCHAR(300)  NULL,
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6)   NOT NULL,
    paid_at            DATETIME(6)   NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    UNIQUE KEY uk_payment_idem (idempotency_key),
    -- 同一厂商订单只能对应一条支付单，防止回调串单
    UNIQUE KEY uk_provider_order (provider, provider_order_id),
    KEY idx_payment_user (user_id, created_at),
    KEY idx_payment_business (business_no),
    KEY idx_payment_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单';

CREATE TABLE payment_transaction (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    payment_id       BIGINT        NOT NULL,
    kind             VARCHAR(16)   NOT NULL COMMENT 'CAPTURE / REFUND',
    provider_txn_id  VARCHAR(64)   NULL COMMENT '厂商流水号',
    amount           DECIMAL(12,2) NOT NULL,
    currency         VARCHAR(8)    NOT NULL,
    status           VARCHAR(20)   NOT NULL COMMENT 'SUCCESS / PENDING / FAILED',
    raw_json         MEDIUMTEXT    NULL COMMENT '厂商原始响应，对账依据',
    created_at       DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_txn_payment (payment_id, created_at),
    -- 同一厂商流水只记一次，重复回调不会重复入账
    UNIQUE KEY uk_txn_provider (provider_txn_id),
    CONSTRAINT fk_txn_payment FOREIGN KEY (payment_id)
        REFERENCES payment_order (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水（捕获/退款）';

CREATE TABLE payment_webhook_event (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    provider           VARCHAR(16) NOT NULL,
    event_id           VARCHAR(80) NOT NULL COMMENT '厂商事件 id，用于幂等去重',
    event_type         VARCHAR(80) NULL,
    payment_no         VARCHAR(32) NULL COMMENT '解析出的我方单号，解析失败可空',
    signature_verified TINYINT(1)  NOT NULL DEFAULT 0,
    raw_body           MEDIUMTEXT  NOT NULL COMMENT '原始报文，验签与对账的唯一依据',
    process_result     VARCHAR(200) NULL,
    received_at        DATETIME(6) NOT NULL,
    processed_at       DATETIME(6) NULL,
    PRIMARY KEY (id),
    -- 回调必然重复投递：唯一键就是幂等的第一道闸门
    UNIQUE KEY uk_webhook_event (provider, event_id),
    KEY idx_webhook_payment (payment_no),
    KEY idx_webhook_time (received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调事件（幂等 + 审计）';

-- 退款单：与捕获分开记，因为一次捕获可能分多次退款
CREATE TABLE payment_refund (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    payment_id        BIGINT        NOT NULL,
    refund_no         VARCHAR(32)   NOT NULL COMMENT '我方退款单号',
    provider_refund_id VARCHAR(64)  NULL,
    amount            DECIMAL(12,2) NOT NULL,
    currency          VARCHAR(8)    NOT NULL,
    status            VARCHAR(20)   NOT NULL COMMENT 'PENDING / SUCCESS / FAILED',
    reason            VARCHAR(300)  NULL,
    operator_name     VARCHAR(80)   NULL COMMENT '发起退款的后台用户名',
    created_at        DATETIME(6)   NOT NULL,
    completed_at      DATETIME(6)   NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_refund_payment (payment_id),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id)
        REFERENCES payment_order (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款单';
