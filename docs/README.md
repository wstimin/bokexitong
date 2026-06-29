# 二次元风格个人博客系统

这是一个基于 Spring Boot 3 + Vue 3 + MySQL 8 的前后端分离个人博客系统，按“前台门户 + 创作中心 + 后台管理”的结构实现。视觉风格偏二次元，图片不内置生成，统一通过后台录入 URL 使用。

## 技术栈

- 后端：Spring Boot 3.2.0、Spring Security、JWT、MyBatis-Plus 3.5.7、Hutool、Lombok
- 数据库：MySQL 8.x。`sql/personal_blog.sql` 是 MySQL 建库建表脚本文件，不是 SQL Server 数据库。
- 前端：Vue 3、Vite、Pinia、Vue Router、Element Plus、ECharts 6、Marked、Highlight.js
- 运行环境：Java 17、Node.js 18+、MySQL 8

## 功能模块

- 前台门户：首页横幅、文章列表、分类、标签云、文章详情、Markdown 渲染、代码高亮
- 互动社区：登录后评论、点赞、收藏
- 创作中心：发布文章、草稿、封面图片 URL、Markdown 正文
- 后台管理：仪表盘、文章管理、分类标签、评论审核、用户封禁/解封
- 图片链接管理：新增、编辑、删除图片 URL，支持 HERO、COVER、AVATAR、RECOMMEND 用途和预览

## 图片 URL 使用方式

后台进入“图片链接”页面，新增图片 URL：

- `HERO`：首页横幅背景，前台首页自动读取第一条启用数据
- `COVER`：可作为文章封面素材
- `AVATAR`：可作为用户头像素材
- `RECOMMEND`：可作为推荐位图片素材

文章发布页也支持直接输入封面图片 URL。

## 本地启动步骤

1. 导入数据库脚本：`sql/personal_blog.sql`
2. 修改后端数据库配置：`backend/src/main/resources/application.yml`
3. 启动后端：在 `backend` 目录执行 `mvn spring-boot:run`
4. 启动前端：在 `frontend` 目录执行 `npm install`，再执行 `npm run dev`
5. 访问前端：`http://localhost:5173`

## 服务器一键拉取部署

新服务器推荐直接执行下面这一条命令，脚本会自动安装基础工具、拉取 GitHub 项目、安装 Docker、生成 `.env`，并启动 MySQL 8、后端和前端：

```bash
curl -fsSL https://raw.githubusercontent.com/wstimin/bokexitong/main/scripts/install.sh | bash
```

默认安装目录是 `/opt/bokexitong`。如果想安装到指定目录，例如宝塔常用目录，可以这样执行：

```bash
curl -fsSL https://raw.githubusercontent.com/wstimin/bokexitong/main/scripts/install.sh | INSTALL_DIR=/www/wwwroot/bokexitong bash
```

如果服务器提示 `curl: command not found`，先安装 curl：

```bash
apt update && apt install -y curl
```

如果项目已经下载到服务器，也可以在项目根目录执行：

```bash
chmod +x scripts/deploy.sh
bash scripts/deploy.sh
```

部署后访问：

- 前端：`http://服务器IP/`
- 后端 API：`http://服务器IP/api/`
- MySQL 8：容器服务名 `mysql`，端口 `3306`

`.env` 中建议修改：

- `MYSQL_ROOT_PASSWORD`：MySQL 8 root 密码
- `BLOG_JWT_SECRET`：JWT 密钥，生产环境请改成长随机字符串

Docker Compose 会启动三个服务：

- `mysql`：使用官方 `mysql:8.0` 镜像，并自动导入 `sql/personal_blog.sql`
- `backend`：Spring Boot 3 后端，连接 MySQL 8
- `frontend`：Nginx 托管 Vue 构建产物，并反向代理 `/api/` 到后端

如果服务器已有 MySQL 8，不想用容器 MySQL，可以只部署后端和前端，并把后端环境变量改成你的 MySQL 8 地址：

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://你的MySQL地址:3306/personal_blog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=你的密码
```

完整部署教程请查看：`docs/deployment.md`，其中包含宝塔部署、1Panel 部署和服务器直接部署三种方式。

默认账号：

- 管理员：`admin / 123456`

纯净安装只初始化管理员账号，分类、标签、图片和文章请在后台自行创建。

## 后端接口简表

- `POST /api/auth/login`：登录
- `GET /api/portal/home`：首页数据
- `GET /api/portal/articles`：公开文章分页
- `GET /api/portal/articles/{id}`：文章详情
- `POST /api/articles`：发布/保存文章
- `GET /api/admin/dashboard`：后台统计
- `GET|POST|DELETE /api/admin/images`：图片 URL 管理
- `GET|POST|DELETE /api/admin/categories`：分类管理
- `GET|POST|DELETE /api/admin/tags`：标签管理

## 说明

本项目适合课程设计、毕业设计雏形、个人博客二次开发。图片素材由你自己提供 URL，系统只负责保存、预览和引用。
