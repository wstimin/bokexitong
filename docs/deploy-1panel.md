# 1Panel 网页部署

1Panel 这一版只走网页，不需要在服务器里手敲命令。推荐直接上传 `bokexitong-linux.tar.gz` 构建包，然后在 1Panel 里把应用和站点跑起来。

这个包已经包含后端 `app.jar`、前端 `dist` 和一份可直接使用的 `.env`。首次打开站点时会自动进入 `/install` 安装向导，填写站点名、域名和管理员密码后即可完成初始化。

## 前提

- 服务器已经安装 1Panel。
- 服务器已经安装 Docker 和 Compose 插件。
- 域名已经解析到服务器 IP。
- 已经放行 `80` 和 `443` 端口。

## 第一步: 上传并解压

在 1Panel 的文件管理里上传 `bokexitong-linux.tar.gz`，解压到你想放的位置，比如：

```text
/opt/bokexitong
```

解压后你会看到这些文件：

```text
docker-compose.yml
.env
backend/app.jar
backend/Dockerfile.runtime
frontend/dist/
frontend/Dockerfile.runtime
sql/personal_blog.sql
```

## 第二步: 启动 Compose

在 1Panel 页面中依次点击：

1. 「容器」
2. 「编排」或「Compose」
3. 「创建编排」
4. 选择项目目录
5. 选择 `docker-compose.yml`
6. 环境变量文件选择 `.env`
7. 点击创建并启动

如果你想让前端只给站点反代使用，可以把 `FRONTEND_HTTP_BIND` 保持为 `18080`；如果直接想临时访问，也可以按需改成 `80`。

## 第三步: 创建网站

在 1Panel 页面中依次点击：

1. 「网站」
2. 「创建网站」
3. 类型选「反向代理」
4. 主域名填你的域名
5. 代理地址填：

```text
http://127.0.0.1:18080
```

6. 保存

## 第四步: 申请 HTTPS

在网站详情里继续点击：

1. 「HTTPS」或「SSL」
2. 申请 Let's Encrypt
3. 选择你的域名
4. 开启 HTTPS
5. 开启 HTTP 自动跳转 HTTPS

## 第五步: 打开安装页

Compose 启动后，访问你的域名：

```text
https://你的域名/
```

如果还没配置 HTTPS，可以先访问：

```text
http://你的域名/
```

首次访问会自动进入 `/install` 安装向导。填写站点名称、访问域名和管理员密码后，系统会自动完成初始化并跳回站点首页。

## 登录信息

后台用户名固定是：

```text
admin
```

后台初始密码来自构建包里的 `.env` 文件，安装后也可以在容器目录里查看。

## 注意

- 不要把后端 `8080` 直接暴露到公网。
- 不要把 MySQL `3306` 直接暴露到公网。
- 网站反向代理地址就是 `http://127.0.0.1:18080`。
