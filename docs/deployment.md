# 部署说明

项目提供两种独立部署路径：

1. `bokexitong-web.tar.gz`：给 1Panel / 宝塔使用。解压后启动 Java 服务，浏览器直接打开网页安装向导。
2. `bokexitong-linux.tar.gz`：给 Linux 一键脚本使用。保留菜单交互和 Compose 部署。

这两种方式彼此独立，请根据服务器环境选择其中一种，不要混用。

## 推荐选择

- 你想要“上传解压后直接打开网页安装向导”，选 `bokexitong-web.tar.gz`。
- 你想要“一键脚本自动拉取、菜单交互、Compose 编排”，选 `bokexitong-linux.tar.gz`。

## 1Panel / 宝塔

详见：

- [1Panel 网页部署](./deploy-1panel.md)
- [宝塔网页部署](./deploy-bt.md)

这条路径的特征是：

- 不需要 Compose。
- 不包含 MySQL，使用你在面板或其他服务器上准备的 MySQL 8 数据库。
- 不包含 `.env`、SQL 初始化文件或预生成运行配置。
- 需要 Java 17 或更高版本。
- 运行目录必须是能够直接看到 `web-start.sh`、`backend/`、`frontend/`、`config/` 和 `uploads/` 的目录。
- 启动命令使用 `/bin/sh web-start.sh`，不要直接运行 `java -jar backend/app.jar`。
- 访问域名后直接进入 `/install`。
- 安装页里填写数据库、站点和管理员密码。
- 1Panel 容器运行环境中，数据库主机应填写实际 MySQL 服务名或容器名，不能默认填写 `127.0.0.1`。
- 安装完成后 Java 进程会退出一次，`web-start.sh` 会在当前运行环境内自动启动正式进程并加载新配置。

面板通用参数：

```text
Java 版本：17
启动命令：/bin/sh web-start.sh
服务端口：18080
反向代理：http://127.0.0.1:18080
```

访问域名由面板网站和反向代理管理，网页安装向导中的“访问域名”可以留空。

## Linux 一键脚本

详见：

- [Linux 一键部署](./deploy-one-click.md)

这条路径的特征是：

- 首次执行安装脚本时自动下载 `bokexitong-linux.tar.gz` 构建包；构建包下载失败时回退到源码部署。
- 使用菜单管理服务、域名和证书。
- 保留 Compose 部署。
- 使用包内 MySQL、SQL 初始化脚本和 `.env`。
- 一键部署会自动标记为已安装，不进入网页安装向导。
- 使用 `shiye-bk update` 更新时需要能够下载最新 Release 构建包；下载失败会停止更新，不会自动改用源码覆盖现有部署。

## 本机验证地址

Web 包默认监听：

```text
127.0.0.1:18080
```

网页安装页：

```text
http://127.0.0.1:18080/install
```

如果域名访问显示 502，优先检查本机地址是否能打开。
