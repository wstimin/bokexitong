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
('heroBadge', '博客', '首页徽标文字'),
('backgroundUrl', '', '全站背景图 URL'),
('logoUrl', '', '站点 Logo 和浏览器图标 URL'),
('seoDescription', '', 'SEO 描述'),
('seoKeywords', '', 'SEO 关键词'),
('icpBeian', '', '备案号'),
('footerText', '', '页脚文字'),
('allowRegister', 'true', '是否开放公开注册'),
('mailEnabled', 'false', '是否启用后台 SMTP 邮件服务'),
('mailHost', '', 'SMTP 服务器地址'),
('mailPort', '587', 'SMTP 端口'),
('mailUsername', '', 'SMTP 登录账号'),
('mailPassword', '', 'SMTP 登录密码或授权码'),
('mailFromName', '博客系统', '邮件发件名称'),
('mailSmtpAuth', 'true', 'SMTP 是否需要认证'),
('mailStarttlsEnable', 'true', 'SMTP 是否启用 STARTTLS'),
('mailSslEnable', 'false', 'SMTP 是否启用 SSL')
ON DUPLICATE KEY UPDATE setting_key = setting_key;

UPDATE site_setting
SET setting_value = '博客'
WHERE setting_key = 'heroBadge' AND setting_value = CHAR(80, 101, 114, 115, 111, 110, 97, 108, 32, 66, 108, 111, 103);
