# 博客系统

这是一个前后端分离的多用户博客系统，包含前台门户、用户中心、文章发布、后台管理、站点设置、图片资源管理、评论管理、违禁词拦截和 Docker Compose 部署配置。

## 部署方式

项目提供三种独立部署方式：

- 一键脚本部署：适合纯净 Linux 服务器，安装后使用 `shiye-bk` 菜单管理。
- 1Panel 部署：上传项目后，在 1Panel 网页向导中创建 Compose 应用、反向代理和证书。
- 宝塔部署：上传项目后，在宝塔网页向导中创建 Compose 应用、反向代理和证书。

详细说明见 [docs/deployment.md](docs/deployment.md)。

## 一键脚本

```bash
curl -fsSL https://raw.githubusercontent.com/wstimin/bokexitong/main/install.sh | bash
```

默认安装目录：

```text
/opt/bokexitong
```

安装完成后执行：

```bash
shiye-bk
```

菜单支持：

- 安装 / 重新部署
- 更新系统
- 卸载系统
- 查看当前用户名密码
- 配置域名
- 申请并启用 SSL 证书
- 查看容器状态

常用子命令：

```bash
shiye-bk status
shiye-bk update
shiye-bk domain example.com
shiye-bk ssl example.com
```

## 初始账号

后台用户名：

```text
admin
```

后台初始密码由部署脚本生成，保存在项目目录 `.env` 中：

```text
BLOG_ADMIN_INITIAL_PASSWORD=...
```

也可以执行：

```bash
shiye-bk status
```

## 服务端口

默认 Docker Compose 只对外开放前端：

```text
80
```

配置域名和证书后，脚本会把前端容器切换为本机监听：

```text
127.0.0.1:18080
```

再由主机 Nginx 接管域名和 HTTPS。

## 项目结构

```text
backend/   Spring Boot 后端服务
frontend/  Vue 3 前端应用
sql/       MySQL 初始化和升级脚本
scripts/   部署、菜单、卸载脚本
docs/      设计和部署文档
```

## 常用命令

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
docker compose restart
docker compose down
```
