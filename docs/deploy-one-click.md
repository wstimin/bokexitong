# 一键脚本部署

一键脚本适合纯净 Linux 服务器。安装完成后会生成全局菜单命令：

```bash
shiye-bk
```

后续安装、更新、卸载、查看当前配置、域名和证书都从这个菜单进入。

一键脚本是一条独立部署路线，不需要你手动上传压缩包。脚本会优先下载 GitHub Release 里的 `bokexitong-linux.tar.gz` 构建包，服务器只会用包里的 `app.jar` 和 `dist` 构建轻量 Docker 运行镜像，不会在服务器上执行 Maven 或 npm 构建。只有构建包下载不到时，脚本才会回退到源码拉取和服务器本地构建。

## 服务器要求

- Ubuntu 22.04、Debian 12、Rocky Linux 9、CentOS Stream 9 或同类 Linux。
- 建议 2 核 4G 以上内存。
- 服务器可以访问 GitHub 和 Docker 镜像源。
- 如果要直接用脚本申请证书，需要开放 `80` 和 `443` 端口。

## 安装

在服务器终端执行：

```bash
curl -fsSL https://raw.githubusercontent.com/wstimin/bokexitong/main/install.sh | bash
```

默认安装目录：

```text
/opt/bokexitong
```

指定安装目录：

```bash
curl -fsSL https://raw.githubusercontent.com/wstimin/bokexitong/main/install.sh | INSTALL_DIR=/opt/shiye-bk bash
```

安装完成后打开菜单：

```bash
shiye-bk
```

## 菜单功能

`shiye-bk` 菜单包含：

1. 安装 / 重新部署
2. 更新系统
3. 卸载系统
4. 查看当前配置
5. 域名 / SSL
6. 查看容器状态

也可以直接执行子命令：

```bash
shiye-bk status
shiye-bk update
shiye-bk domain example.com
shiye-bk ssl example.com
shiye-bk uninstall
```

## 域名 / SSL

进入后可以先配置域名，再申请证书。

### 配置域名

确保域名已经解析到服务器 IP，然后执行：

```bash
shiye-bk domain example.com
```

脚本会自动做三件事：

1. 把 `.env` 里的 `BLOG_DOMAIN` 写成你的域名。
2. 把前端容器改为只监听本机 `127.0.0.1:18080`。
3. 在主机 Nginx 中创建反向代理，把域名转发到 `http://127.0.0.1:18080`。

### 申请并启用证书

域名解析生效后执行：

```bash
shiye-bk ssl example.com
```

脚本会安装 Certbot，申请 Let's Encrypt 证书，并让 Nginx 自动启用 HTTPS 和 HTTP 跳转。

如果需要指定证书通知邮箱：

```bash
SSL_EMAIL=admin@example.com shiye-bk ssl example.com
```

## 查看当前配置

```bash
shiye-bk status
```

会显示：

- 项目目录
- 当前域名
- 前端监听端口
- 前台访问网址
- 后台访问网址
- 后台用户名
- 后台初始密码
- 当前容器状态

## 更新

```bash
shiye-bk update
```

更新会下载最新 `bokexitong-linux.tar.gz`，保留 `.env`、数据库卷和上传文件卷，然后重新启动服务并自动执行数据库升级脚本。老版本如果曾经是源码目录安装，执行 `shiye-bk update` 后也会切换到 Release 构建包更新路线。

配置域名或申请证书不需要重新安装。`shiye-bk domain example.com` 和 `shiye-bk ssl example.com` 只会热更新 `.env`、前端监听端口和 Nginx 反向代理。

## 卸载

```bash
shiye-bk uninstall
```

菜单会询问是否彻底卸载：

- 普通卸载：停止并删除容器，保留数据库卷、上传文件卷和项目文件。
- 彻底卸载：删除容器、数据库卷、上传文件卷、`.env` 和项目目录。
- 如果之前配置过域名 / SSL，彻底卸载还会清理本项目创建的 Nginx 站点配置，并在确认是本项目安装的 Nginx 时停止它。

彻底卸载会删除数据，执行前请确认已经备份。

## 常用排查

```bash
cd /opt/bokexitong
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

如果页面打不开，先看 `docker compose ps`。如果接口报错，先看 `backend` 日志。如果是域名和证书问题，先确认域名解析、服务器安全组和 `80/443` 端口。

## 一键卸载

如果想彻底清理本项目并重新安装，可以直接执行：

```bash
curl -fsSL https://raw.githubusercontent.com/wstimin/bokexitong/main/uninstall.sh | bash -s -- --purge -y
```

执行后再重新安装即可：

```bash
curl -fsSL https://raw.githubusercontent.com/wstimin/bokexitong/main/install.sh | bash
```
