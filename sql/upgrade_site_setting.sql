USE personal_blog;

CREATE TABLE IF NOT EXISTS site_setting (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  setting_key VARCHAR(80) NOT NULL UNIQUE,
  setting_value TEXT,
  description VARCHAR(255),
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO site_setting(setting_key, setting_value, description) VALUES
('siteName', '博客系统', '站点名称'),
('heroTitle', '博客系统', '首页大标题'),
('heroSubtitle', '用卡片浏览公开文章，点开后再阅读完整内容。登录后可以进入用户中心创作、管理自己的文章。', '首页说明'),
('heroBadge', 'Personal Blog', '首页徽标文字'),
('backgroundUrl', '', '全站背景图 URL'),
('allowRegister', 'true', '是否开放公开注册')
ON DUPLICATE KEY UPDATE setting_key = setting_key;
