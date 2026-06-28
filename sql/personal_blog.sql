CREATE DATABASE IF NOT EXISTS personal_blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE personal_blog;

DROP TABLE IF EXISTS favorite;
DROP TABLE IF EXISTS like_record;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS article_tag;
DROP TABLE IF EXISTS image_resource;
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
  email VARCHAR(120),
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
  status VARCHAR(20) DEFAULT 'APPROVED',
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
  type VARCHAR(30) NOT NULL COMMENT 'HERO/COVER/AVATAR/RECOMMEND',
  description VARCHAR(255),
  sort INT DEFAULT 0,
  enabled TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_image_type(type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO blog_user(username, nickname, password, avatar, email, role, status)
VALUES ('admin', '站长', '{noop}123456', '', 'admin@example.com', 'ADMIN', 1),
       ('demo', '演示用户', '{noop}123456', '', 'demo@example.com', 'USER', 1);

INSERT INTO category(name, description, sort)
VALUES ('日常随笔', '生活碎片与灵感记录', 1),
       ('技术笔记', 'Java、Vue、数据库与工具链', 2),
       ('动漫杂谈', '番剧感想与角色分析', 3);

INSERT INTO tag(name, color)
VALUES ('SpringBoot3', '#60a5fa'), ('Vue3', '#3ad9c9'), ('Markdown', '#ff77b7'), ('二次元', '#a78bfa');

INSERT INTO article(user_id, category_id, title, summary, cover_url, content, content_type, status, view_count, like_count, favorite_count, published_at)
VALUES
(1, 2, '用 Spring Boot 3 和 Vue 3 搭一个二次元博客', '从接口、权限、文章创作到后台仪表盘，记录一个前后端分离博客系统的搭建过程。', '', '# 开篇\n\n这是一个支持 Markdown、代码高亮、评论互动和后台管理的个人博客系统。\n\n```java\nSystem.out.println("Hello Blog");\n```', 'MARKDOWN', 'PUBLISHED', 128, 18, 9, NOW()),
(1, 3, '给博客加一点番剧感 UI', '用柔和色彩、徽章标签、横幅图和卡片动效，让内容系统更有个人气质。', '', '## 设计思路\n\n二次元风格不等于堆满元素，关键是色彩、节奏和细节统一。', 'MARKDOWN', 'PUBLISHED', 86, 12, 5, NOW());

INSERT INTO article_tag(article_id, tag_id) VALUES (1, 1), (1, 2), (1, 3), (2, 4);

INSERT INTO image_resource(title, url, type, description, sort, enabled)
VALUES ('首页横幅占位图', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1600&q=80', 'HERO', '后台可替换为你自己的二次元横幅 URL', 1, 1);
