# 博客系统

这是一个前后端分离的多用户博客系统，包含前台门户、用户中心、文章发布、后台管理、站点配置、图片资源管理、评论管理和违禁词拦截。

## 部署方式

项目提供两种独立部署方式：

1. `bokexitong-linux.tar.gz`：给纯 Linux 服务器的一键脚本使用。保留菜单式交互和 Compose 编排部署。
2. `bokexitong-web.tar.gz`：给 1Panel / 宝塔使用。解压后启动一个 Java Web 服务，浏览器直接打开站点根路径就进入网页安装向导。这里不需要 Compose，也不需要一键脚本。

两种方式彼此独立，请根据服务器环境选择其中一种，不要混用。

## Linux 一键脚本

Linux 一键脚本继续保留菜单交互和 Compose 部署。该版本包含 MySQL Compose 服务、SQL 初始化脚本和 `.env.example`；首次部署时会在服务器生成 `.env`，不使用 Web 包的网页安装向导。

安装：

```bash
curl -fsSL https://raw.githubusercontent.com/wstimin/bokexitong/main/install.sh | bash
```

默认安装目录：

```text
/opt/bokexitong
```

安装完成后运行：

```bash
shiye-bk
```

一键脚本首次部署会自动生成数据库密码、JWT 密钥和管理员初始密码，并写入 `/opt/bokexitong/.env`。脚本会把数据库初始化为已安装状态，因此首次启动不会进入 `/install`。

完整说明见 [Linux 一键部署](./docs/deploy-one-click.md)。

## 1Panel / 宝塔 Web 包

Web 包特点：

- 解压后直接启动后端 Java 服务。
- 站点根路径直接返回网页。
- 首次访问自动进入 `/install` 安装向导。
- 安装向导里填写数据库连接、站点信息和管理员密码。
- 安装完成后会写入 `config/bokexitong-runtime.properties`，`web-start.sh` 会在当前容器或运行环境内自动重启 Java 并加载新配置。

### 构建部署包

在有 Node.js、Maven 和 tar 的机器上执行：

```bash
bash scripts/build-package.sh
```

脚本会同时生成两个文件：

```text
package/bokexitong-linux.tar.gz
package/bokexitong-web.tar.gz
```

其中，`bokexitong-web.tar.gz` 只用于 1Panel / 宝塔；`bokexitong-linux.tar.gz` 只用于 Linux 一键脚本。Web 包不包含 Docker Compose、MySQL、SQL 初始化文件、`.env` 或预生成运行配置。

正式安装包发布在 [GitHub Releases](https://github.com/wstimin/bokexitong/releases/latest)，仓库 `package/` 目录只保留下载说明和更新清单，避免旧二进制安装包与源码版本不一致。

### 1Panel 部署步骤

1. 在 1Panel 文件管理里上传 `bokexitong-web.tar.gz`。
2. 解压到网站文件目录，运行目录指向能够直接看到 `web-start.sh` 的这一层。
3. 创建 Java 17 运行环境，启动命令填写 `/bin/sh web-start.sh`，端口填写 `18080`。
4. 运行用户填写 `root`，容器名称可填写 `bokexitong`。
5. 在 1Panel 里创建网站，类型选反向代理。
6. 反向代理目标填写 `http://127.0.0.1:18080`。
7. 绑定域名并申请证书。
8. 访问你的域名，直接进入网页安装向导。

不要把宿主机绝对路径写进启动命令，也不要直接运行 `java -jar backend/app.jar`。完整字段说明见 [1Panel 网页安装部署](./docs/deploy-1panel.md)。

### 宝塔部署步骤

1. 在宝塔文件管理里上传 `bokexitong-web.tar.gz`。
2. 解压到网站目录，例如 `/www/wwwroot/你的域名/bokexitong-web`。
3. 创建 Java 17 项目，运行目录指向能够直接看到 `web-start.sh` 的这一层。
4. 启动命令填写 `/bin/sh web-start.sh`，端口填写 `18080`。
5. 创建网站，类型选反向代理。
6. 代理目标填 `http://127.0.0.1:18080`。
7. 绑定域名并申请证书。
8. 访问域名后会直接进入安装向导。

运行用户必须对 `config/` 和 `uploads/` 有写权限。完整字段说明见 [宝塔网页安装部署](./docs/deploy-bt.md)。

### 安装向导填写

- 数据库主机：1Panel 容器运行环境填写实际 MySQL 服务名或容器名，例如 `MySQL`，不能默认填写 `127.0.0.1`；宝塔宿主机进程可按实际环境填写 `127.0.0.1` 或应用可访问的数据库地址。
- 数据库端口：`3306`。
- 数据库名：`bokexitong`，必须与面板中创建的数据库名一致。
- 数据库用户：`bokexitong`，必须与面板中创建的数据库用户一致，建议不要使用 `root`。
- 数据库密码：填写该数据库用户的真实密码。用户至少需要在该数据库中创建数据表的权限。
- 站点名称：自定义。
- 访问域名：可以留空，面板网站和反向代理已经负责域名访问。
- 管理员密码：至少 8 位。

安装完成后，系统会保存运行配置并让 Java 进程退出一次。`web-start.sh` 会立即使用新配置重新启动 Java，页面会在服务恢复后自动进入正常站点。

### 本机访问地址

如果你想先在服务器本机验证，不经过域名反代，直接访问：

```text
http://127.0.0.1:18080/
```

安装页地址是：

```text
http://127.0.0.1:18080/install
```

### 502 排查

如果域名打开后是 502，按这个顺序查：

1. 先在服务器本机确认 `http://127.0.0.1:18080/` 能打开。
2. 如果本机能开，说明 Java 服务正常，是面板反向代理没连上。
3. 反向代理目标先用 `http://127.0.0.1:18080`。
4. 如果面板环境访问不了回环地址，再改成服务器内网 IP。
5. 如果本机也打不开，查看 `web-start.sh` 启动日志。

## 后台账号

后台用户名固定为：

```text
admin
```

后台初始密码：

- Web 包：在网页安装向导里设置。
- 一键脚本：由脚本安装时生成，保存在项目目录 `.env` 中。

## 常用命令

```bash
shiye-bk status
shiye-bk update
shiye-bk domain example.com
shiye-bk ssl example.com
```

## 后台一键更新

后台“版本与更新”页面支持检查更新并一键安装。更新清单通过 HTTPS 获取，安装包在替换前必须通过 SHA-256 校验。

- Linux / Docker Compose 包由独立的 `updater` 服务执行更新，数据库、上传文件和 `.env` 不会被覆盖。
- 1Panel / 宝塔 Web 包由 `web-start.sh` 托管进程，并在应用退出码为 `42` 时调用内置更新代理后重新启动。
- 源码调试环境默认使用 `disabled` 模式，不会修改开发目录；生产部署包会自动启用对应代理。

更新代理拥有替换应用文件和重启服务所需的高权限，因此只有管理员接口能够创建更新任务。推送已递增版本号的 `main` 分支后，GitHub Actions 会自动测试、构建两个安装包、生成匹配的 `update-manifest.json`，并发布为 Latest Release。仓库中的清单由发布任务自动同步，用于兼容仍读取旧地址的历史版本。

## 版本迭代与推送规范

项目使用统一语义化版本号，根 Maven 工程、后端、前端和页面兜底版本必须保持一致。每次向已有远端分支推送代码前都必须提升版本号。

```bash
# 按改动范围选择其一
node scripts/version.mjs patch
node scripts/version.mjs minor
node scripts/version.mjs major

# 只检查所有版本号是否一致
node scripts/version.mjs verify
```

仓库内置 `.githooks/pre-push`。首次克隆后执行下面的命令启用；当前维护仓库已经启用：

```bash
git config core.hooksPath .githooks
```

如果本次版本号没有高于远端版本，`git push` 会被直接阻止。GitHub Actions 还会再次执行版本策略、前端测试与构建、后端测试与打包，避免绕过本地钩子。
