# 宝塔网页安装部署

本方式只使用 `bokexitong-web.tar.gz`，不使用 Docker Compose，也不要执行 Linux 一键部署脚本。部署完成后访问域名会直接进入网页安装向导。

## 1. 准备数据库

在宝塔“数据库”中准备 MySQL 8 数据库，例如：

```text
数据库名：bokexitong
用户名：bokexitong
密码：使用宝塔生成或自己设置的强密码
```

如果数据库用户没有建库权限，必须先创建好数据库；网页安装向导会自动创建数据表。

## 2. 上传并解压

把 `bokexitong-web.tar.gz` 上传到网站目录并解压，例如：

```text
/www/wwwroot/bk.example.com/bokexitong-web
```

作为运行目录的文件夹中必须直接包含：

```text
backend/app.jar
frontend/dist/
config/
uploads/
web-start.sh
```

如果解压后多了一层 `bokexitong-web`，运行目录必须指向这一层，不能指向它的上级目录。

## 3. 创建 Java 项目或进程守护

使用 Java 17。推荐填写：

```text
项目名称：bokexitong
项目路径：/www/wwwroot/bk.example.com/bokexitong-web
启动命令：/bin/sh web-start.sh
运行目录：/www/wwwroot/bk.example.com/bokexitong-web
端口：18080
运行用户：www
```

如果使用的宝塔版本要求启动命令填写绝对路径，可以使用：

```sh
/bin/sh /www/wwwroot/bk.example.com/bokexitong-web/web-start.sh
```

注意：

- 推荐通过 `/bin/sh web-start.sh` 启动，不要直接执行 `java -jar backend/app.jar`。
- 运行用户必须对 `config/` 和 `uploads/` 有写权限。
- 如果使用 `www` 用户，目录权限可设置为归属 `www:www`；不要对整个目录使用 `777`。
- 不需要额外填写环境变量，脚本默认监听 `127.0.0.1:18080`。

如果 `config/` 和 `uploads/` 当前不属于 `www` 用户，可在终端执行：

```sh
chown -R www:www /www/wwwroot/bk.example.com/bokexitong-web/config
chown -R www:www /www/wwwroot/bk.example.com/bokexitong-web/uploads
chmod 755 /www/wwwroot/bk.example.com/bokexitong-web/web-start.sh
```

请把示例域名路径替换成实际项目路径，不要直接对整个项目目录设置 `777`。

## 4. 配置反向代理

在宝塔网站设置中添加反向代理：

```text
代理地址：http://127.0.0.1:18080
```

域名由宝塔网站管理，不需要在 Java 项目中重复填写。保存后访问网站域名，应直接出现网页安装向导。

## 5. 网页安装向导填写

| 网页字段 | 填写内容 |
| --- | --- |
| 数据库主机 | MySQL 在宿主机运行时通常填 `127.0.0.1`；在容器中运行时填实际服务名或宿主机可达地址 |
| 端口 | `3306` |
| 数据库名 | `bokexitong`，与宝塔创建的数据库名一致 |
| 数据库用户 | `bokexitong`，与宝塔创建的数据库用户一致 |
| 数据库密码 | 该数据库用户的真实密码 |
| 站点名称 | 自定义，例如 `博客系统` |
| 访问域名 | 可以留空，宝塔反向代理已经负责域名访问 |
| 管理员密码 | 至少 8 位，这是后台 `admin` 用户的密码 |

点击安装后会生成：

```text
config/bokexitong-runtime.properties
```

随后首次 Java 进程会退出，`web-start.sh` 会在当前运行环境内自动加载新配置并重新启动 Java。页面会等待服务恢复后自动进入首页，正常情况下不需要手动重启。

## 6. 常见错误

### 502 Bad Gateway

检查 Java 项目是否启动、端口是否为 `18080`，并确认反向代理目标是：

```text
http://127.0.0.1:18080
```

### 数据库 Connection refused

说明 Java 无法连接网页中填写的数据库主机和端口。检查数据库地址、端口、防火墙和 MySQL 授权范围。

### 无法生成运行配置

检查运行用户是否能写入：

```text
config/
uploads/
```

## 7. 更新和备份

必须保留并备份：

```text
config/
uploads/
```

更新时只替换 `backend/`、`frontend/`、`web-start.sh` 和 `README.md`，不要覆盖运行配置和上传文件。替换后在宝塔中重启一次 Java 项目。
