USE personal_blog;

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT,
  operator_name VARCHAR(80),
  action VARCHAR(40) NOT NULL,
  target_type VARCHAR(40) NOT NULL,
  target_id BIGINT,
  detail VARCHAR(1000),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_operation_log_created(created_at),
  INDEX idx_operation_log_action(action),
  INDEX idx_operation_log_target(target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
