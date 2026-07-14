# 个人博客系统

这是一个前后端分离的个人博客系统：后端使用 Spring Boot 3 提供 REST API，前端使用 Vue 3 + Vite 构建门户、用户中心和后台管理页面，数据库使用 MySQL 8。

## 模块边界

- 前台门户：公开访问，负责文章卡片浏览、分类筛选、标签筛选、搜索、文章详情、评论展示。
- 用户中心：登录用户访问，负责个人资料维护、密码修改、文章发布、草稿和本人文章管理。
- 后台管理：管理员访问，负责文章、分类、标签、评论、用户和图片资源管理。

## 技术栈

- 后端：Spring Boot 3.2、Spring Security、JWT、MyBatis-Plus、Lombok
- 前端：Vue 3、Vite、Vue Router、Pinia、Element Plus、marked、highlight.js
- 数据库：MySQL 8
- 部署：Docker Compose、Nginx

## 目录结构

```text
backend/   Spring Boot 后端服务
frontend/  Vue/Vite 前端应用
sql/       MySQL 初始化脚本
docs/      项目说明和设计文档
scripts/   部署脚本
```

## 接口分组

- `/api/auth/**`：登录、邮箱验证码注册、邮箱验证码重置密码。
- `/api/portal/**`：公开门户接口，只返回公开文章和公开展示数据。
- `/api/users/**`：当前登录用户资料。
- `/api/articles/**`：用户文章操作和管理员文章管理。
- `/api/comments/**`：评论列表与评论发布。
- `/api/admin/**`：后台管理接口，只允许管理员访问。
- `/api/uploads/**`：登录用户上传文件，公开读取上传资源。

## 当前前台文章体验

首页采用卡片浏览方式。文章列表接口返回轻量卡片数据，包括标题、摘要、封面、分类、标签、作者、浏览量、点赞数和收藏数，不返回正文。用户点击卡片进入详情页后，才加载 Markdown 正文和评论区。

## 本地启动

1. 导入数据库脚本：`sql/personal_blog.sql`
2. 修改后端数据库配置：`backend/src/main/resources/application.yml`
3. 启动后端：在 `backend` 目录执行 `mvn spring-boot:run`
4. 启动前端：在 `frontend` 目录执行 `npm install`，然后执行 `npm run dev`
5. 访问前端：`http://localhost:5173`

本地初始化管理员用户名为 `admin`。首次部署到生产环境时，一键部署脚本会生成初始管理员密码并写入 `.env` 的 `BLOG_ADMIN_INITIAL_PASSWORD`；手动部署时请自行设置该环境变量，生产环境不会允许继续使用默认密码。

## 后续建议

- 将上传文件迁移到对象存储，并在后台图片资源中统一管理。
- 补充后端单元测试和前端页面级测试。
