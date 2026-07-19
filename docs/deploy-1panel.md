# 1Panel 网页安装部署

本方式只使用 `bokexitong-web.tar.gz`。不使用 Docker Compose，也不要执行一键部署脚本。部署完成后访问域名会直接进入网页安装向导。

## 1. 准备环境

- 1Panel 已安装 OpenResty。
- 1Panel 已安装 Java 17 运行环境。
- 已在 1Panel 中安装 MySQL 8。
- 已创建网站并绑定域名。
- 已准备数据库名、数据库用户和数据库密码。

建议先在 1Panel 的“数据库”中创建：

```text
数据库名：bokexitong
用户名：bokexitong
密码：使用 1Panel 生成或自己设置的强密码
```

如果数据库用户没有建库权限，必须先创建好数据库；网页向导仍会自动创建数据表。

## 2. 上传和解压构建包

进入 1Panel 网站的文件目录。实际目录通常类似：

```text
/opt/1panel/apps/openresty/openresty/www/sites/你的域名/index
```

把 `bokexitong-web.tar.gz` 上传并解压。压缩包自带一层 `bokexitong-web` 目录，必须满足下面两种方式中的一种：

### 方式 A：把包内文件放到网站 index 目录

最终结构：

```text
index/
├── backend/app.jar
├── frontend/dist/
├── config/
├── uploads/
└── web-start.sh
```

运行目录填写网站的 `index` 目录。

### 方式 B：保留 bokexitong-web 子目录

最终结构：

```text
index/bokexitong-web/
├── backend/app.jar
├── frontend/dist/
├── config/
├── uploads/
└── web-start.sh
```

运行目录必须填写 `index/bokexitong-web`，不能仍然填写 `index`。

判断标准：在运行目录中必须能直接看到 `web-start.sh`，同时能看到 `backend`、`frontend`、`config` 和 `uploads`。

## 3. 创建 Java 运行环境

进入 1Panel 的“网站 -> 运行环境”，创建 Java 运行环境。按照实际界面填写：

| 填写项 | 填写内容 |
| --- | --- |
| 名称 | `bokexitong` |
| 运行目录 | 网站中直接包含 `web-start.sh` 的目录 |
| 启动命令 | `/bin/sh web-start.sh` |
| 端口 | `18080` |
| 用户 | `root` |
| 容器名称 | `bokexitong` |

以域名 `bk.example.com` 且文件直接放在 `index` 中为例：

```text
名称：bokexitong
运行目录：/opt/1panel/apps/openresty/openresty/www/sites/bk.example.com/index
启动命令：/bin/sh web-start.sh
端口：18080
用户：root
容器名称：bokexitong
```

重要说明：

- 启动命令必须填写 `/bin/sh web-start.sh`。
- 不要填写 `/opt/1panel/.../web-start.sh` 这样的宿主机绝对路径。Java 运行环境在容器中运行时，该绝对路径可能不存在。
- 不要直接填写 `java -jar backend/app.jar`。只有通过 `web-start.sh` 启动，网页安装完成后 Java 才能在当前容器中自动重启并加载新配置。
- 使用 `/bin/sh web-start.sh` 时不依赖脚本执行位；当前构建包也已经把 `web-start.sh` 设置为 `755`。
- 使用 `root` 运行时不需要额外调整文件所有者；如果改用其他用户，该用户必须能够写入 `config/` 和 `uploads/`。
- 不需要额外填写环境变量，脚本默认监听 `127.0.0.1:18080`。

保存后启动运行环境。日志中出现以下内容表示 Java 已启动：

```text
Started ... Application
Tomcat started on port 18080
```

## 4. 设置网站反向代理

在 1Panel 网站设置中添加反向代理：

```text
代理地址：http://127.0.0.1:18080
```

保存后直接访问已经绑定的网站域名。访问域名字段不需要在运行环境中再次填写，域名由 1Panel 网站和反向代理负责。

如果域名访问显示 502：

1. 确认运行环境状态为“已启动”。
2. 确认运行环境端口填写的是 `18080`。
3. 确认反向代理目标是 `http://127.0.0.1:18080`。
4. 查看运行环境日志是否出现 Java 启动异常。

## 5. 网页安装向导填写

首次访问域名会自动进入 `/install`。按照下面填写：

| 网页字段 | 填写内容 |
| --- | --- |
| 数据库主机 | `MySQL`，或 1Panel 中实际的 MySQL 服务/容器名称 |
| 端口 | `3306` |
| 数据库名 | `bokexitong`，必须与 1Panel 创建的数据库名一致 |
| 数据库用户 | `bokexitong`，必须与 1Panel 创建的数据库用户一致 |
| 数据库密码 | 1Panel 中该数据库用户的真实密码 |
| 站点名称 | 自定义，例如 `博客系统` |
| 访问域名 | 可以留空；1Panel 已设置反向代理时不影响访问 |
| 管理员密码 | 至少 8 位，这是后台 `admin` 用户的密码 |

数据库主机特别说明：

- 当前这种 1Panel Java 运行环境是容器方式，`127.0.0.1` 表示 Java 容器自身，不是 MySQL 容器。
- Java 和 MySQL 在同一个 Docker 网络时，填写 MySQL 的服务名或容器名。你当前测试环境使用的是 `MySQL`。
- 不要把数据库名 `bokexitong` 当成数据库主机填写。
- 如果 1Panel 中 MySQL 的实际服务名不是 `MySQL`，必须使用面板显示的实际名称。

点击“开始安装”后，系统会：

1. 连接 MySQL 并创建数据表。
2. 创建或更新后台用户 `admin`。
3. 写入 `config/bokexitong-runtime.properties`。
4. 退出首次 Java 进程。
5. 由 `web-start.sh` 在当前容器内自动启动正式 Java 进程。
6. 页面检测到服务恢复后自动进入首页。

正常情况下不需要手动重启运行环境或容器。

## 6. 安装失败排查

### 页面提示后端服务不可用

检查运行环境是否启动、端口是否为 `18080`、反向代理是否为 `http://127.0.0.1:18080`。

### 日志显示 Connection refused

这是 Java 容器无法连接填写的数据库主机和端口。检查：

- 数据库主机是否为实际 MySQL 服务名，例如 `MySQL`。
- MySQL 是否正在运行。
- Java 运行环境和 MySQL 是否在同一个 Docker 网络。
- MySQL 是否监听 `3306`。

连接失败时不会生成：

```text
config/bokexitong-runtime.properties
```

### 安装后短暂显示 502

Java 正在加载新配置重启，页面会自动轮询恢复状态。一般等待数秒即可，不要重复提交安装表单。

## 7. 更新和备份

必须保留并备份：

```text
config/
uploads/
```

更新构建包时，只替换：

```text
backend/
frontend/
web-start.sh
README.md
```

不要删除或覆盖 `config/bokexitong-runtime.properties` 和 `uploads/`，否则会丢失数据库连接配置或用户上传文件。替换完成后，在 1Panel 中重启一次运行环境即可。
