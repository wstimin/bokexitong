# 部署文档

本项目提供三种独立部署方式，请按你的服务器环境选择一种，不要把三种方式混在一起操作。

现在推荐使用构建包部署。构建包名为 `bokexitong-linux.tar.gz`，里面已经包含后端 `app.jar` 和前端 `dist`，服务器启动时不需要再执行 Maven 或 npm 构建。

## 选择方式

- 一键脚本部署：适合纯净 Linux 服务器，直接执行安装命令。脚本会优先下载 GitHub Release 的构建包，下载不到时自动回退到源码构建，安装后使用 `shiye-bk` 菜单完成更新、卸载、域名和证书配置。
- 1Panel 部署：上传 `bokexitong-linux.tar.gz` 后，全程在 1Panel 网页里完成 Compose、站点、证书和首次安装向导。
- 宝塔部署：上传 `bokexitong-linux.tar.gz` 后，全程在宝塔网页里完成 Compose、站点、证书和首次安装向导。

这三种方式是分开的：一键脚本不需要你手动上传压缩包；1Panel / 宝塔不需要执行一键脚本。

## 文档入口

- [一键脚本部署](./deploy-one-click.md)
- [1Panel 部署](./deploy-1panel.md)
- [宝塔部署](./deploy-bt.md)

## 默认服务

项目运行后包含三个服务：

- `mysql`：MySQL 8 数据库。
- `backend`：Spring Boot 后端接口，容器内端口 `8080`。
- `frontend`：Nginx 前端站点，容器内端口 `80`，并代理 `/api/` 到后端。

生产环境不要把 MySQL `3306` 和后端 `8080` 直接暴露到公网。

## 初始账号

后台初始用户名固定为：

```text
admin
```

初始密码由部署脚本生成，保存在项目目录的 `.env` 文件中：

```text
BLOG_ADMIN_INITIAL_PASSWORD=...
```

如果使用一键脚本部署，也可以执行下面命令查看：

```bash
shiye-bk status
```
