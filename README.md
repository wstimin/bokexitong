# 博客系统

这是一个前后端分离的多用户博客系统，包含前台门户、用户中心、文章发布、后台管理、邮箱验证码、站点设置、图片资源管理和 Docker Compose 部署配置。

## 一键部署

在一台新的 Linux 服务器上执行下面命令即可自动拉取代码、安装 Docker、生成生产环境配置并启动服务：

```bash
curl -fsSL https://raw.githubusercontent.com/wstimin/bokexitong/main/install.sh | bash
```

默认安装目录：

```text
/opt/bokexitong
```

指定安装目录：

```bash
curl -fsSL https://raw.githubusercontent.com/wstimin/bokexitong/main/install.sh | INSTALL_DIR=/www/wwwroot/bokexitong bash
```

部署完成后访问：

```text
http://服务器IP/
```

初始管理员：

```text
username: admin
password: 部署脚本结束时会打印，也会保存在服务器项目目录的 .env 文件中
```

生产环境不会继续使用 `admin / 123456`。一键部署会生成真实初始密码并写入 `.env` 的 `BLOG_ADMIN_INITIAL_PASSWORD`。

## 已经有代码时部署

如果你已经把项目克隆到了服务器：

```bash
git clone https://github.com/wstimin/bokexitong.git
cd bokexitong
bash deploy.sh
```

后续更新也可以执行：

```bash
cd /opt/bokexitong
git pull
bash deploy.sh
```

## 手动 Docker 部署

不使用一键脚本时，需要手动填写真实环境变量：

```bash
cp .env.example .env
vi .env
docker compose up -d --build
```

`.env` 里必须填写真实值：

```env
MYSQL_ROOT_PASSWORD=填写真实强密码
BLOG_JWT_SECRET=填写至少32位的随机字符串
BLOG_ADMIN_INITIAL_PASSWORD=填写至少8位的管理员初始密码
BLOG_MAIL_ENABLED=false
BLOG_MAIL_FROM_NAME=博客系统
SPRING_MAIL_HOST=
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=
SPRING_MAIL_SMTP_AUTH=true
SPRING_MAIL_STARTTLS_ENABLE=true
SPRING_MAIL_SSL_ENABLE=false
```

如果开启邮箱验证码，把 `BLOG_MAIL_ENABLED` 改成 `true`，并填写真实 SMTP：

```env
BLOG_MAIL_ENABLED=true
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_USERNAME=你的SMTP账号
SPRING_MAIL_PASSWORD=你的SMTP密码或授权码
```

注册和找回密码会发送真实邮箱验证码，不提供测试验证码。

## 服务端口

Docker Compose 默认只对外开放前端：

```text
80
```

MySQL 和后端 API 默认不直接暴露到公网。前端 Nginx 会把 `/api/` 请求转发到后端容器。

## 常用命令

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
docker compose restart
docker compose down
```

## 项目结构

```text
backend/   Spring Boot 后端服务
frontend/  Vue 3 前端应用
sql/       MySQL 初始化和升级脚本
scripts/   实际部署脚本
docs/      详细设计和部署文档
```

详细部署说明见 [docs/deployment.md](docs/deployment.md)。
