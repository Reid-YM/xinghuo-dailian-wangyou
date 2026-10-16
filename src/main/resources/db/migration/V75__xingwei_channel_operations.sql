-- Additive internal channel operations only. No external platform writes or payment changes.
CREATE TABLE xw_channel_status (
    channel_code VARCHAR(16) NOT NULL PRIMARY KEY,
    service_state VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    note VARCHAR(500) NOT NULL DEFAULT '',
    version BIGINT NOT NULL DEFAULT 0,
    updated_by VARCHAR(64) NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO xw_channel_status (channel_code) VALUES ('WEBSITE'), ('TAOBAO'), ('XIANYU');

CREATE TABLE xw_channel_tasks (
    id CHAR(36) NOT NULL PRIMARY KEY,
    channel_code VARCHAR(16) NOT NULL,
    title VARCHAR(120) NOT NULL,
    details VARCHAR(2000) NOT NULL DEFAULT '',
    status VARCHAR(16) NOT NULL DEFAULT 'TODO',
    owner_admin_user_id BIGINT NOT NULL,
    department_code VARCHAR(16) NULL,
    due_date DATE NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_channel_tasks_owner (owner_admin_user_id, updated_at),
    INDEX idx_channel_tasks_department (department_code, updated_at),
    INDEX idx_channel_tasks_status (channel_code, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
