CREATE TABLE admin_ai_chat_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  message_role VARCHAR(16) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(16) NOT NULL,
  model VARCHAR(80) NULL,
  error_text VARCHAR(1000) NULL,
  created_by VARCHAR(100) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_ai_chat_created (created_at),
  INDEX idx_ai_chat_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
