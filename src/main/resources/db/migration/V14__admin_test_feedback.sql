CREATE TABLE admin_test_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  page_path VARCHAR(240) NULL,
  content TEXT NOT NULL,
  priority VARCHAR(16) NOT NULL,
  status VARCHAR(24) NOT NULL,
  operator_note VARCHAR(1000) NULL,
  created_by VARCHAR(100) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  INDEX idx_feedback_status_updated (status, updated_at),
  INDEX idx_feedback_priority_created (priority, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
