# 二次元博客系统部署文档

本文档适用于将本项目部署到 Linux 服务器，数据库使用 **MySQL 8.x**。项目结构为前后端分离：

- 后端：Spring Boot 3，默认端口 `8080`，接口前缀 `/api`
- 前端：Vue 3，构建后由 Nginx 托管
- 数据库：MySQL 8，数据库名 `personal_blog`
- 初始化脚本：`sql/personal_blog.sql`

默认账号：

- 管理员：`admin / 123456`
- 普通用户：`demo / 123456`

## 一、部署前准备

服务器建议配置：

- 系统：Ubuntu 22.04、Debian 12、CentOS 7/8、Rocky Linux 9 均可
- CPU：2 核及以上
- 内存：2GB 及以上，推荐 4GB
- 磁盘：20GB 及以上
- 放行端口：`80`、`443`、`8080`，MySQL 端口 `3306` 不建议公网开放

生产环境建议：

- 使用域名绑定前端站点
- 后端只允许本机或内网访问，由 Nginx 反向代理 `/api/`
- 修改 `.env` 或后端配置中的 JWT 密钥
- 修改默认管理员密码
- MySQL 8 密码使用强密码

## 二、服务器命令行环境安装

如果不使用宝塔或 1Panel，只通过服务器命令部署，需要先安装运行环境。本项目有两种部署方式：

- Docker Compose 部署：只需要安装 Docker 和 Docker Compose，最推荐
- 手动部署：需要安装 JDK 17、Maven、Node.js、Nginx、MySQL 8

### 1. 查看服务器系统版本

先登录服务器：

```bash
ssh root@你的服务器IP
```

查看系统：

```bash
cat /etc/os-release
uname -m
```

如果是国内服务器，建议先更新软件源并安装常用工具。

Ubuntu / Debian：

```bash
apt update
apt install -y curl wget git vim unzip tar ca-certificates gnupg lsb-release software-properties-common
```

CentOS / Rocky / AlmaLinux：

```bash
yum install -y curl wget git vim unzip tar ca-certificates
```

Rocky 9 / AlmaLinux 9 也可以使用：

```bash
dnf install -y curl wget git vim unzip tar ca-certificates
```

### 2. 方式一：安装 Docker 和 Docker Compose

如果你准备用项目根目录的 `docker-compose.yml` 部署，只需要安装 Docker。

项目已经提供一键部署脚本，上传项目到服务器后可以直接执行：

```bash
chmod +x scripts/deploy.sh
bash scripts/deploy.sh
```

脚本会自动检查 Docker、创建 `.env`、构建并启动 MySQL 8、后端和前端服务。下面的 Docker 安装命令适合需要手动安装或排查环境时使用。

Ubuntu / Debian：

```bash
curl -fsSL https://get.docker.com | bash
systemctl enable docker
systemctl start docker
docker -v
docker compose version
```

CentOS / Rocky / AlmaLinux：

```bash
yum install -y yum-utils
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable docker
systemctl start docker
docker -v
docker compose version
```

如果服务器拉取 Docker 镜像较慢，可以配置镜像加速。创建或编辑：

```bash
mkdir -p /etc/docker
vim /etc/docker/daemon.json
```

写入示例：

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io"
  ]
}
```

重启 Docker：

```bash
systemctl daemon-reload
systemctl restart docker
```

Docker Compose 部署时，MySQL 8、JDK 17、Node.js、Nginx 都在容器中提供，服务器本机不需要额外安装这些环境。

### 3. 方式二：手动部署所需环境

如果不用 Docker，需要安装以下环境：

| 环境 | 推荐版本 | 用途 |
| --- | --- | --- |
| JDK | 17 | 运行 Spring Boot 3 后端 |
| Maven | 3.8+ | 后端打包 |
| Node.js | 18+，推荐 20 LTS | 前端依赖安装和打包 |
| npm | 9+ | 前端包管理 |
| MySQL | 8.x | 数据库 |
| Nginx | 1.20+ | 托管前端、反向代理接口 |
| Git | 任意稳定版 | 拉取代码，可选 |

### 4. Ubuntu / Debian 手动安装命令

更新系统工具：

```bash
apt update
apt install -y curl wget git vim unzip tar ca-certificates gnupg lsb-release
```

安装 JDK 17：

```bash
apt install -y openjdk-17-jdk
java -version
```

安装 Maven：

```bash
apt install -y maven
mvn -version
```

安装 Node.js 20 LTS：

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs
node -v
npm -v
```

配置 npm 国内镜像，可选但推荐：

```bash
npm config set registry https://registry.npmmirror.com
npm config get registry
```

安装 Nginx：

```bash
apt install -y nginx
systemctl enable nginx
systemctl start nginx
nginx -v
```

安装 MySQL 8：

```bash
apt install -y mysql-server
systemctl enable mysql
systemctl start mysql
mysql --version
```

Ubuntu 20.04 / 22.04 默认通常是 MySQL 8。确认版本：

```bash
mysql -uroot -p -e "SELECT VERSION();"
```

如果你的系统源不是 MySQL 8，可以使用 Docker 单独运行 MySQL 8，或安装 MySQL 官方 APT 源。Docker 单独运行 MySQL 8 示例：

```bash
docker run -d \
  --name anime-blog-mysql \
  --restart always \
  -e MYSQL_ROOT_PASSWORD=你的强密码 \
  -e MYSQL_DATABASE=personal_blog \
  -p 127.0.0.1:3306:3306 \
  -v anime-blog-mysql-data:/var/lib/mysql \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

### 5. CentOS / Rocky / AlmaLinux 手动安装命令

安装基础工具：

```bash
yum install -y curl wget git vim unzip tar ca-certificates
```

安装 JDK 17：

```bash
yum install -y java-17-openjdk java-17-openjdk-devel
java -version
```

安装 Maven：

```bash
yum install -y maven
mvn -version
```

如果系统源没有 Maven，使用二进制方式安装 Maven 3.9：

```bash
cd /opt
wget https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
tar -zxvf apache-maven-3.9.9-bin.tar.gz
ln -s /opt/apache-maven-3.9.9 /opt/maven
cat >/etc/profile.d/maven.sh <<'EOF'
export MAVEN_HOME=/opt/maven
export PATH=$MAVEN_HOME/bin:$PATH
EOF
source /etc/profile.d/maven.sh
mvn -version
```

安装 Node.js 20 LTS：

```bash
curl -fsSL https://rpm.nodesource.com/setup_20.x | bash -
yum install -y nodejs
node -v
npm -v
npm config set registry https://registry.npmmirror.com
```

安装 Nginx：

```bash
yum install -y nginx
systemctl enable nginx
systemctl start nginx
nginx -v
```

安装 MySQL 8 官方源：

```bash
yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-11.noarch.rpm
yum install -y mysql-community-server
systemctl enable mysqld
systemctl start mysqld
mysql --version
```

CentOS 7 首次安装 MySQL 8 后会生成临时 root 密码，查看：

```bash
grep 'temporary password' /var/log/mysqld.log
```

登录并修改密码：

```bash
mysql -uroot -p
```

在 MySQL 中执行：

```sql
ALTER USER 'root'@'localhost' IDENTIFIED BY '你的强密码';
```

Rocky 9 / AlmaLinux 9 如果安装 MySQL 官方源遇到 GPG Key 问题，可先导入官方密钥：

```bash
rpm --import https://repo.mysql.com/RPM-GPG-KEY-mysql-2023
```

### 6. 配置防火墙

Ubuntu / Debian 常用 UFW：

```bash
ufw allow 22
ufw allow 80
ufw allow 443
ufw enable
ufw status
```

CentOS / Rocky / AlmaLinux 常用 firewalld：

```bash
systemctl enable firewalld
systemctl start firewalld
firewall-cmd --permanent --add-service=ssh
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --reload
firewall-cmd --list-all
```

生产环境不建议开放 MySQL `3306` 到公网。如果只是后端本机访问 MySQL，保持 `3306` 只监听本机或内网即可。

### 7. 环境安装完成检查

手动部署前，建议执行：

```bash
java -version
mvn -version
node -v
npm -v
mysql --version
nginx -v
```

推荐结果：

```text
Java 17.x
Maven 3.8+
Node.js 18+ 或 20+
npm 9+
MySQL 8.x
Nginx 1.20+
```

## 三、宝塔面板部署

宝塔部署适合不熟悉命令行的服务器用户。推荐使用“前端静态站点 + 后端 Java 项目 + MySQL 8”的方式部署。

### 1. 安装环境

在宝塔软件商店安装：

- Nginx 1.22+
- MySQL 8.0
- Java 项目管理器，或安装 JDK 17
- PM2 管理器可选，前端不需要 Node 常驻运行

如果宝塔软件商店没有 JDK 17，可以在服务器命令行安装：

```bash
apt update
apt install -y openjdk-17-jdk maven nodejs npm
```

CentOS/Rocky 系统可使用：

```bash
yum install -y java-17-openjdk java-17-openjdk-devel maven nodejs npm
```

### 2. 创建 MySQL 8 数据库

进入宝塔：数据库 -> 添加数据库。

建议填写：

- 数据库名：`personal_blog`
- 用户名：`personal_blog`
- 密码：填写强密码
- 访问权限：本地服务器

创建后，进入 phpMyAdmin 或宝塔数据库管理，导入：

```text
sql/personal_blog.sql
```

注意：这个文件是 MySQL 8 建库建表脚本，不是 SQL Server。

### 3. 修改后端配置

编辑文件：

```text
backend/src/main/resources/application-prod.yml
```

如果使用宝塔创建的数据库，建议后端启动时通过环境变量传入，不直接写死密码：

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/personal_blog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=personal_blog
SPRING_DATASOURCE_PASSWORD=你的数据库密码
BLOG_JWT_SECRET=请改成至少32位的随机字符串
```

### 4. 打包后端

在服务器项目目录执行：

```bash
cd backend
mvn clean package -DskipTests
```

打包产物一般位于：

```text
backend/target/blog-backend-1.0.0.jar
```

实际文件名以 `target` 目录为准。

### 5. 宝塔添加 Java 项目

打开宝塔：网站 -> Java 项目 -> 添加 Java 项目。

填写参考：

- 项目类型：Spring Boot
- 项目 jar：选择 `backend/target/*.jar`
- 运行端口：`8080`
- JDK 版本：17
- 启动参数：

```bash
--spring.profiles.active=prod
```

环境变量填写：

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/personal_blog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=personal_blog
SPRING_DATASOURCE_PASSWORD=你的数据库密码
BLOG_JWT_SECRET=请改成至少32位的随机字符串
```

启动后访问测试：

```text
http://服务器IP:8080/api/portal/home
```

### 6. 打包前端

在服务器项目目录执行：

```bash
cd frontend
npm install --registry=https://registry.npmmirror.com
npm run build
```

构建产物在：

```text
frontend/dist
```

### 7. 宝塔添加前端站点

打开宝塔：网站 -> 添加站点。

填写参考：

- 域名：你的域名，或先填服务器 IP
- 根目录：选择 `frontend/dist`
- PHP 版本：纯静态

然后进入站点设置 -> 配置文件，添加 Vue 路由和 API 反向代理配置：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}

location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

保存后重载 Nginx。

### 8. 宝塔部署检查

访问：

```text
http://你的域名/
```

登录后台：

```text
http://你的域名/login
```

默认管理员：`admin / 123456`。

进入后台“图片链接”页面，新增 `HERO` 类型图片 URL，首页会自动使用。

## 四、1Panel 部署

1Panel 推荐使用 Docker Compose 部署，最省心，也最适合后续迁移。

### 1. 上传项目

在 1Panel 中进入：文件，上传整个项目到服务器，例如：

```text
/opt/anime-blog
```

也可以用 Git 拉取：

```bash
cd /opt
git clone 你的仓库地址 anime-blog
cd anime-blog
```

### 2. 修改环境变量

在项目根目录复制环境变量文件：

```bash
cp .env.example .env
```

编辑 `.env`：

```env
MYSQL_ROOT_PASSWORD=请改成强密码
BLOG_JWT_SECRET=请改成至少32位的随机字符串
```

### 3. 使用 1Panel 编排部署

进入 1Panel：容器 -> 编排 -> 创建编排。

选择项目根目录中的：

```text
docker-compose.yml
```

点击启动。该编排会启动：

- `anime-blog-mysql`：MySQL 8.0
- `anime-blog-backend`：Spring Boot 后端
- `anime-blog-frontend`：Nginx 前端

MySQL 首次启动会自动执行：

```text
sql/personal_blog.sql
```

### 4. 1Panel 网站反向代理

如果直接使用 `docker-compose.yml` 中的 `80:80`，访问服务器 IP 即可。

如果你想通过 1Panel 的“网站”统一管理域名和 HTTPS，可以这样做：

1. 修改 `docker-compose.yml`，将前端端口改成内部端口，例如：

```yaml
ports:
  - "18080:80"
```

2. 在 1Panel 网站中创建反向代理站点：

```text
http://127.0.0.1:18080
```

3. 绑定域名并申请 HTTPS 证书。

### 5. 1Panel 部署检查

查看容器日志：

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

访问：

```text
http://你的域名/
```

API 测试：

```text
http://你的域名/api/portal/home
```

## 五、服务器直接部署

服务器直接部署分两种：Docker Compose 一键部署，以及不使用 Docker 的手动部署。

### 方案 A：Docker Compose 一键部署

这是最推荐的服务器部署方式。

### 1. 安装 Docker

Ubuntu/Debian：

```bash
curl -fsSL https://get.docker.com | bash
systemctl enable docker
systemctl start docker
```

检查版本：

```bash
docker -v
docker compose version
```

### 2. 上传项目并配置环境变量

```bash
cd /opt
# 上传项目到 /opt/anime-blog 后进入目录
cd /opt/anime-blog
cp .env.example .env
vi .env
```

修改：

```env
MYSQL_ROOT_PASSWORD=请改成强密码
BLOG_JWT_SECRET=请改成至少32位的随机字符串
```

### 3. 启动服务

```bash
docker compose up -d --build
```

也可以直接使用项目的一键部署脚本：

```bash
chmod +x scripts/deploy.sh
bash scripts/deploy.sh
```

查看服务状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f
```

访问：

```text
http://服务器IP/
```

### 4. Docker 常用运维命令

重启：

```bash
docker compose restart
```

停止：

```bash
docker compose down
```

更新代码后重新构建：

```bash
docker compose up -d --build
```

备份 MySQL：

```bash
docker exec anime-blog-mysql mysqldump -uroot -p personal_blog > personal_blog_backup.sql
```

恢复 MySQL：

```bash
docker exec -i anime-blog-mysql mysql -uroot -p personal_blog < personal_blog_backup.sql
```

### 方案 B：不使用 Docker 的手动部署

适合已有 MySQL 8、Nginx、JDK 17 的服务器。

### 1. 安装依赖

Ubuntu/Debian：

```bash
apt update
apt install -y openjdk-17-jdk maven nodejs npm nginx mysql-server
```

如果系统源中的 MySQL 不是 8.x，请安装 MySQL 官方 8.x 源，或使用 Docker 部署 MySQL 8。

### 2. 初始化 MySQL 8

登录 MySQL：

```bash
mysql -uroot -p
```

执行：

```sql
CREATE DATABASE IF NOT EXISTS personal_blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'personal_blog'@'localhost' IDENTIFIED BY '你的强密码';
GRANT ALL PRIVILEGES ON personal_blog.* TO 'personal_blog'@'localhost';
FLUSH PRIVILEGES;
```

导入脚本：

```bash
mysql -uroot -p personal_blog < sql/personal_blog.sql
```

### 3. 打包并启动后端

```bash
cd backend
mvn clean package -DskipTests
```

创建运行目录：

```bash
mkdir -p /opt/anime-blog/backend
cp target/*.jar /opt/anime-blog/backend/blog-backend.jar
```

创建 systemd 服务：

```bash
vi /etc/systemd/system/anime-blog-backend.service
```

写入：

```ini
[Unit]
Description=Anime Blog Backend
After=network.target mysql.service

[Service]
WorkingDirectory=/opt/anime-blog/backend
ExecStart=/usr/bin/java -jar /opt/anime-blog/backend/blog-backend.jar --spring.profiles.active=prod
Restart=always
RestartSec=5
Environment=SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/personal_blog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
Environment=SPRING_DATASOURCE_USERNAME=personal_blog
Environment=SPRING_DATASOURCE_PASSWORD=你的数据库密码
Environment=BLOG_JWT_SECRET=请改成至少32位的随机字符串

[Install]
WantedBy=multi-user.target
```

启动后端：

```bash
systemctl daemon-reload
systemctl enable anime-blog-backend
systemctl start anime-blog-backend
systemctl status anime-blog-backend
```

查看日志：

```bash
journalctl -u anime-blog-backend -f
```

### 4. 打包并部署前端

```bash
cd frontend
npm install --registry=https://registry.npmmirror.com
npm run build
```

复制前端产物：

```bash
mkdir -p /var/www/anime-blog
cp -r dist/* /var/www/anime-blog/
```

配置 Nginx：

```bash
vi /etc/nginx/conf.d/anime-blog.conf
```

写入：

```nginx
server {
    listen 80;
    server_name 你的域名或服务器IP;

    root /var/www/anime-blog;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

检查并重载 Nginx：

```bash
nginx -t
systemctl reload nginx
```

### 5. 配置 HTTPS

如果服务器使用域名，推荐使用 acme.sh 或面板自带证书功能配置 HTTPS。

宝塔和 1Panel 都可以直接在网站设置里申请 Let's Encrypt 证书。

命令行服务器也可以使用 certbot：

```bash
apt install -y certbot python3-certbot-nginx
certbot --nginx -d 你的域名
```

## 六、部署后必做事项

### 1. 修改默认密码

登录后台后，建议尽快修改管理员密码。当前项目已预置用户表字段，后续可在用户中心补充密码修改接口，或直接在 MySQL 中更新密码。

### 2. 配置图片链接

进入后台：图片链接管理。

新增一条：

- 类型：`HERO`
- URL：你的首页二次元横幅图片地址
- 启用：是

前台首页会自动读取启用状态下排序最靠前的 `HERO` 图片。

文章封面可以在创作中心直接填写图片 URL，也可以先在后台图片链接里维护 `COVER` 类型素材。

### 3. 不要开放 MySQL 公网端口

如果使用 Docker Compose，`docker-compose.yml` 默认映射了 `3306:3306`，方便调试。正式生产环境建议删除或注释这段：

```yaml
ports:
  - "3306:3306"
```

后端容器可以通过 Docker 内部网络访问 MySQL，不需要公网开放 3306。

### 4. 设置防火墙

只开放必要端口：

```bash
ufw allow 80
ufw allow 443
ufw allow 22
ufw enable
```

如果后端不需要公网直接访问，可以不开放 `8080`。

## 七、常见问题

### 1. 前端能打开，但接口 404

检查 Nginx 是否配置了：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
}
```

Docker Compose 部署时，前端容器内应代理到：

```nginx
proxy_pass http://backend:8080/api/;
```

### 2. 后端启动失败，提示连接数据库失败

检查：

- MySQL 是否为 8.x
- 数据库名是否为 `personal_blog`
- 用户名和密码是否正确
- JDBC URL 是否带 `allowPublicKeyRetrieval=true`
- Docker 部署时后端应连接 `mysql:3306`，非 Docker 部署时通常连接 `127.0.0.1:3306`

### 3. 登录提示用户名或密码错误

确认是否已导入初始化脚本：

```bash
mysql -uroot -p personal_blog < sql/personal_blog.sql
```

默认管理员为：

```text
admin / 123456
```

### 4. 刷新前端页面 404

这是 Vue Router history 模式常见问题。Nginx 需要配置：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

### 5. 首页没有二次元横幅图

后台图片链接管理中新增 `HERO` 类型图片 URL，并确保启用。没有配置时，前端会显示 CSS 兜底背景。

## 八、推荐部署方式

如果是正式卖源码或给用户部署，推荐优先级：

1. 1Panel + Docker Compose：最省事，环境一致，方便迁移
2. 宝塔：适合面板用户，前后端分开管理直观
3. 服务器直接命令行部署：适合有 Linux 运维经验的用户
