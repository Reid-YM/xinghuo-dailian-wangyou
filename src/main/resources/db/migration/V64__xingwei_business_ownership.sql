-- 星维管理后台 V2D：询价/订单统一业务归属。
-- 仅新增归属表，不修改 quotes、customer_orders、admin_users 或旧后台授权边界。
-- 询价与订单共享 business_no，因此同一归属记录会在询价转订单后自然继承。

CREATE TABLE xw_business_ownership (
    business_no VARCHAR(20) NOT NULL,
    owner_admin_user_id BIGINT NULL,
    department_id BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (business_no),
    KEY idx_xw_business_ownership_owner (owner_admin_user_id),
    KEY idx_xw_business_ownership_department (department_id),
    CONSTRAINT fk_xw_business_ownership_owner FOREIGN KEY (owner_admin_user_id)
        REFERENCES admin_users (id) ON DELETE SET NULL,
    CONSTRAINT fk_xw_business_ownership_department FOREIGN KEY (department_id)
        REFERENCES xw_departments (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='星维询价订单统一业务归属';
