# 宝塔部署

宝塔部署和一键脚本部署是两种方式。已经使用宝塔管理网站、Nginx、域名和证书时，建议让项目容器只监听本机端口，再用宝塔网站的网页向导做反向代理和 SSL。

## 准备

- 服务器已经安装宝塔面板。
- 宝塔已安装 Docker 或 Docker 管理器。
- 宝塔已安装 Nginx。
- 域名已经解析到服务器 IP。
- 开放 `80` 和 `443` 端口。

## 上传项目

任选一种方式：

- 在宝塔「文件」中上传项目压缩包，解压到 `/www/wwwroot/bokexitong`。
- 在宝塔终端执行：

```bash
git clone https://github.com/wstimin/bokexitong.git /www/wwwroot/bokexitong
```

复制环境变量文件：

```bash
cd /www/wwwroot/bokexitong
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

## 网页向导启动应用

如果宝塔有 Docker Compose 管理入口，按下面操作：

1. 打开「Docker」。
2. 进入「Compose」或「编排」。
3. 点击「添加项目」。
4. 项目路径选择 `/www/wwwroot/bokexitong`。
5. Compose 文件选择 `docker-compose.yml`。
6. 环境变量文件选择 `.env`。
7. 保存并启动。

如果当前宝塔没有 Compose 网页入口，可以在宝塔终端启动：

```bash
cd /www/wwwroot/bokexitong
docker compose --env-file .env up -d --build
```

启动后确认容器正常：

```bash
docker compose --env-file .env ps
```

## 网页向导配置站点

在宝塔中创建网站：

1. 打开「网站」。
2. 点击「添加站点」。
3. 域名填写你的域名。
4. PHP 版本选择「纯静态」。
5. 保存后进入站点设置。
6. 打开「反向代理」。
7. 添加反向代理，目标 URL 填写：

```text
http://127.0.0.1:18080
```

8. 保存并启用反向代理。

## 网页向导申请 HTTPS

在宝塔站点设置里操作：

1. 打开站点设置。
2. 进入「SSL」。
3. 选择 Let's Encrypt。
4. 勾选你的域名。
5. 点击申请。
6. 申请成功后开启「强制 HTTPS」。

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

在宝塔终端执行：

```bash
cd /www/wwwroot/bokexitong
git pull
docker compose --env-file .env up -d --build
```

如果需要自动执行数据库升级脚本，执行：

```bash
bash scripts/deploy.sh
```

## 注意事项

- 宝塔 Nginx 已经占用 `80/443` 时，项目容器不要再映射公网 `80`。
- 推荐使用 `FRONTEND_HTTP_BIND=127.0.0.1:18080`。
- 不要开放 MySQL `3306` 到公网。
- 不要开放后端 `8080` 到公网。
