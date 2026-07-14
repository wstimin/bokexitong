USE personal_blog;

CREATE TABLE IF NOT EXISTS email_code (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(120) NOT NULL,
  code_hash VARCHAR(120) NOT NULL,
  scene VARCHAR(40) NOT NULL COMMENT 'REGISTER/RESET_PASSWORD',
  used TINYINT NOT NULL DEFAULT 0,
  expires_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  used_at DATETIME,
  INDEX idx_email_code_lookup(email, scene, used, expires_at),
  INDEX idx_email_code_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO site_setting(setting_key, setting_value, description)
VALUES ('allowRegister', 'true', '是否开放公开注册')
ON DUPLICATE KEY UPDATE setting_key = setting_key;
