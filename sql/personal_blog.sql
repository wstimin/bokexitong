CREATE DATABASE IF NOT EXISTS personal_blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE personal_blog;

DROP TABLE IF EXISTS favorite;
DROP TABLE IF EXISTS like_record;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS article_tag;
DROP TABLE IF EXISTS email_code;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS image_resource;
DROP TABLE IF EXISTS site_setting;
DROP TABLE IF EXISTS article;
DROP TABLE IF EXISTS tag;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS blog_user;

CREATE TABLE blog_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  nickname VARCHAR(80) NOT NULL,
  password VARCHAR(120) NOT NULL,
  avatar VARCHAR(500),
  email VARCHAR(120) NOT NULL UNIQUE,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  description VARCHAR(255),
  sort INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  color VARCHAR(20) DEFAULT '#ff77b7',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE article (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category_id BIGINT,
  title VARCHAR(180) NOT NULL,
  summary VARCHAR(500),
  cover_url VARCHAR(800),
  content LONGTEXT,
  content_type VARCHAR(20) DEFAULT 'MARKDOWN',
  status VARCHAR(20) DEFAULT 'DRAFT',
  view_count INT DEFAULT 0,
  like_count INT DEFAULT 0,
  favorite_count INT DEFAULT 0,
  published_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_article_status(status),
  INDEX idx_article_category(category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE article_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  UNIQUE KEY uk_article_tag(article_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT 0,
  content VARCHAR(1000) NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_comment_article(article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE like_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_like(user_id, article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_favorite(user_id, article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE image_resource (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  url VARCHAR(800) NOT NULL,
  type VARCHAR(30) NOT NULL COMMENT 'LOGO/HERO/BACKGROUND/COVER/AVATAR/RECOMMEND',
  description VARCHAR(255),
  sort INT DEFAULT 0,
  enabled TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_image_type(type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE site_setting (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  setting_key VARCHAR(80) NOT NULL UNIQUE,
  setting_value TEXT,
  description VARCHAR(255),
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE operation_log (
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

CREATE TABLE email_code (
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

INSERT INTO blog_user(username, nickname, password, avatar, email, role, status)
VALUES ('admin', '站长', '{noop}123456', '', 'admin@example.com', 'ADMIN', 1);

INSERT INTO site_setting(setting_key, setting_value, description) VALUES
('siteName', '博客系统', '站点名称'),
('heroTitle', '博客系统', '首页大标题'),
('heroSubtitle', '用卡片浏览公开文章，点开后再阅读完整内容。登录后可以进入用户中心创作、管理自己的文章。', '首页说明'),
('heroBadge', 'Personal Blog', '首页徽标文字'),
('backgroundUrl', '', '全站背景图 URL'),
('allowRegister', 'true', '是否开放公开注册');
