# 博客系统 Web 安装包

此包用于 1Panel 或宝塔，不包含 Docker Compose、MySQL、SQL 初始化文件、`.env` 或预生成运行配置。

## 运行要求

- Java 17 或更高版本
- 可从服务器访问的 MySQL 8 数据库
- 面板进程守护或 Java 项目管理
- 运行用户对 `config/` 和 `uploads/` 有写权限

## 启动

将整个目录解压到网站目录。面板中的运行目录必须设置为能够直接看到以下文件和目录的这一层：

```text
backend/app.jar
frontend/dist/
config/
uploads/
web-start.sh
```

启动命令必须填写：

```sh
/bin/sh web-start.sh
```

不要填写宿主机上的 `/opt/1panel/.../web-start.sh` 绝对路径，也不要直接使用 `java -jar backend/app.jar`。只有通过 `web-start.sh` 启动，安装完成后才能在当前运行环境内自动加载新配置并重启 Java。

服务默认只监听：

```text
127.0.0.1:18080
```

将网站反向代理到 `http://127.0.0.1:18080`，访问域名后按网页安装向导填写数据库和管理员信息。

安装完成时 Java 进程会退出一次，`web-start.sh` 会立即在当前运行环境内重新启动 Java，并加载新生成的数据库与 JWT 配置，不需要手动重启容器或 Java 服务。

## 安装向导填写

```text
数据库端口：3306
数据库名：bokexitong（与面板中创建的数据库一致）
数据库用户：bokexitong（与面板中创建的用户一致）
数据库密码：该用户的真实密码
访问域名：可以留空
管理员密码：至少 8 位
```

- 1Panel Java 运行环境使用容器时，数据库主机填写实际 MySQL 服务名或容器名，例如 `MySQL`，不能默认填写 `127.0.0.1`。
- 宝塔 Java 进程和 MySQL 都直接运行在宿主机时，数据库主机通常填写 `127.0.0.1`；如果任一服务在容器中，应填写应用实际可访问的服务名或地址。
- 数据库用户需要有创建数据表的权限。如果用户没有建库权限，请先在面板中创建 `bokexitong` 数据库。
- 安装成功后会生成 `config/bokexitong-runtime.properties`；连接数据库失败时不会生成该文件。

需要持久化和备份的目录：

```text
config/
uploads/
```
