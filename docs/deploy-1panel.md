# 1Panel 部署

1Panel 部署和一键脚本部署是两种方式。已经使用 1Panel 管理网站、域名和证书时，建议把项目作为 Docker Compose 应用运行，再用 1Panel 的网页向导配置反向代理和 HTTPS。

## 准备

- 服务器已经安装 1Panel。
- 1Panel 已经安装 Docker / Compose 运行环境。
- 域名已经解析到服务器 IP。
- 开放 `80` 和 `443` 端口。

## 上传项目

任选一种方式上传：

- 在 1Panel 文件管理中上传项目压缩包，然后解压到 `/opt/bokexitong`。
- 在 1Panel 终端中执行 `git clone https://github.com/wstimin/bokexitong.git /opt/bokexitong`。

进入项目目录后复制环境变量文件：

```bash
cd /opt/bokexitong
cp .env.example .env
```

编辑 `.env`，至少填写：

```env
MYSQL_ROOT_PASSWORD=你的强密码
BLOG_JWT_SECRET=至少32位随机字符串
BLOG_ADMIN_INITIAL_PASSWORD=至少8位后台初始密码
FRONTEND_HTTP_BIND=127.0.0.1:18080
BLOG_DOMAIN=你的域名
```

`FRONTEND_HTTP_BIND=127.0.0.1:18080` 表示容器只在本机提供 HTTP 服务，由 1Panel 网站反向代理到它。

## 网页向导启动应用

在 1Panel 中按下面步骤操作：

1. 打开「容器」。
2. 进入「编排」或「Compose」。
3. 点击「创建编排」。
4. 选择项目目录 `/opt/bokexitong`。
5. Compose 文件选择 `docker-compose.yml`。
6. 环境变量文件选择 `.env`。
7. 点击创建并启动。

启动后确认三个服务都正常：

- `bokexitong-mysql`
- `bokexitong-backend`
- `bokexitong-frontend`

## 网页向导配置站点

在 1Panel 中创建网站：

1. 打开「网站」。
2. 点击「创建网站」。
3. 类型选择「反向代理」。
4. 主域名填写你的域名。
5. 代理地址填写：

```text
http://127.0.0.1:18080
```

6. 保存后访问 `http://你的域名/`。

## 网页向导申请 HTTPS

在 1Panel 网站详情里操作：

1. 打开刚创建的网站。
2. 进入「HTTPS」或「SSL」。
3. 选择申请 Let's Encrypt 证书。
4. 选择你的域名。
5. 申请成功后开启 HTTPS。
6. 开启 HTTP 自动跳转 HTTPS。

## 后台账号

后台用户名：

```text
admin
```

后台密码查看 `.env`：

```text
BLOG_ADMIN_INITIAL_PASSWORD=...
```

## 更新

在 1Panel 终端执行：

```bash
cd /opt/bokexitong
git pull
docker compose --env-file .env up -d --build
```

更新后如果有数据库升级脚本，也可以执行项目部署脚本：

```bash
bash scripts/deploy.sh
```

## 注意事项

- 不要把后端 `8080` 直接暴露到公网。
- 不要把 MySQL `3306` 直接暴露到公网。
- 1Panel 负责域名和证书时，项目容器前端建议保持 `127.0.0.1:18080`。
