# 1Panel 网页向导部署

1Panel 这一版只走网页，不需要在服务器里手敲命令。你只要上传项目，然后在 1Panel 里点几下就能完成部署、反向代理和 HTTPS。

## 先准备

- 服务器已经安装 1Panel。
- 服务器已经安装 Docker 和 Compose 插件。
- 域名已经解析到服务器 IP。
- 已经放行 `80` 和 `443` 端口。

## 第一步: 上传项目

在 1Panel 的文件管理里上传项目压缩包，解压到你想放的位置，比如：

```text
/opt/bokexitong
```

## 第二步: 创建应用

在 1Panel 页面中依次点击：

1. 「容器」
2. 「编排」或「Compose」
3. 「创建编排」
4. 选择项目目录
5. 选择 `docker-compose.yml`
6. 选择 `.env`
7. 点击创建并启动

## 第三步: 填写配置

打开 `.env`，把这几项填好：

```env
MYSQL_ROOT_PASSWORD=你的强密码
BLOG_JWT_SECRET=至少32位随机字符串
BLOG_ADMIN_INITIAL_PASSWORD=至少8位后台初始密码
BLOG_DOMAIN=你的域名
FRONTEND_HTTP_BIND=127.0.0.1:18080
```

`FRONTEND_HTTP_BIND=127.0.0.1:18080` 的意思是：前端只在本机监听，后面交给 1Panel 的网站反向代理。

## 第四步: 创建网站

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

## 第五步: 申请 HTTPS

在网站详情里继续点击：

1. 「HTTPS」或「SSL」
2. 申请 Let's Encrypt
3. 选择你的域名
4. 开启 HTTPS
5. 开启 HTTP 自动跳转 HTTPS

## 登录信息

后台用户名固定是：

```text
admin
```

后台初始密码在 `.env` 里：

```text
BLOG_ADMIN_INITIAL_PASSWORD=...
```

## 你会看到什么

- 1Panel 负责应用编排
- 1Panel 负责域名
- 1Panel 负责证书
- 项目前端只监听本机 `127.0.0.1:18080`

## 注意

- 不要把后端 `8080` 直接暴露到公网。
- 不要把 MySQL `3306` 直接暴露到公网。
- 网站反向代理地址就是 `http://127.0.0.1:18080`。
