#!/usr/bin/env bash
set -euo pipefail

APP_NAME="bokexitong"
REPO_URL="${REPO_URL:-https://github.com/wstimin/bokexitong.git}"
BRANCH="${BRANCH:-main}"
DEFAULT_INSTALL_DIR="/opt/bokexitong"

SOURCE_PATH="${0:-}"
if [ "${BASH_SOURCE+x}" = "x" ] && [ "${#BASH_SOURCE[@]}" -gt 0 ]; then
  SOURCE_PATH="${BASH_SOURCE[0]}"
fi
if [ -n "$SOURCE_PATH" ] && [ -f "$SOURCE_PATH" ]; then
  SCRIPT_DIR="$(cd "$(dirname "$SOURCE_PATH")" && pwd 2>/dev/null || pwd)"
else
  SCRIPT_DIR="$(pwd)"
fi

if [ -f "$SCRIPT_DIR/../docker-compose.yml" ]; then
  PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
else
  PROJECT_DIR="${INSTALL_DIR:-$DEFAULT_INSTALL_DIR}"
fi
ENV_FILE="$PROJECT_DIR/.env"
NGINX_SITE_NAME="shiye-bk-${APP_NAME}"

info() { printf '\033[1;34m[信息]\033[0m %s\n' "$*"; }
ok() { printf '\033[1;32m[完成]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[提醒]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[失败]\033[0m %s\n' "$*"; exit 1; }

has_cmd() { command -v "$1" >/dev/null 2>&1; }

sudo_cmd() {
  if [ "${EUID:-$(id -u)}" -eq 0 ]; then
    "$@"
  else
    sudo "$@"
  fi
}

pause() {
  [ -t 0 ] || return 0
  printf '\n按 Enter 返回菜单...' > /dev/tty
  read -r _ < /dev/tty || true
}

prompt() {
  message="$1"
  default_value="${2:-}"
  answer=""
  if [ -n "$default_value" ]; then
    printf '%s [%s]: ' "$message" "$default_value" > /dev/tty
  else
    printf '%s: ' "$message" > /dev/tty
  fi
  read -r answer < /dev/tty || true
  printf '%s' "${answer:-$default_value}"
}

confirm() {
  message="$1"
  answer=""
  if [ -r /dev/tty ]; then
    printf '%s [y/N]: ' "$message" > /dev/tty
    read -r answer < /dev/tty || true
  else
    printf '%s [y/N]: ' "$message"
    read -r answer || true
  fi
  case "$answer" in
    y|Y|yes|YES) return 0 ;;
    *) return 1 ;;
  esac
}

ensure_project_dir() {
  [ -d "$PROJECT_DIR" ] || fail "项目目录不存在：$PROJECT_DIR。请先执行安装。"
  [ -f "$PROJECT_DIR/docker-compose.yml" ] || fail "未找到 docker-compose.yml：$PROJECT_DIR"
}

env_value() {
  key="$1"
  [ -f "$ENV_FILE" ] || return 0
  grep -E "^${key}=" "$ENV_FILE" | tail -n 1 | cut -d= -f2-
}

set_env_value() {
  key="$1"
  value="$2"
  ensure_project_dir
  if [ ! -f "$ENV_FILE" ]; then
    touch "$ENV_FILE"
    chmod 600 "$ENV_FILE" || true
  fi
  tmp_file="${ENV_FILE}.tmp.$$"
  awk -v key="$key" -v value="$value" '
    BEGIN { replaced = 0 }
    $0 ~ "^" key "=" { print key "=" value; replaced = 1; next }
    { print }
    END { if (!replaced) print key "=" value }
  ' "$ENV_FILE" > "$tmp_file"
  mv "$tmp_file" "$ENV_FILE"
  chmod 600 "$ENV_FILE" || true
}

compose_cmd() {
  ensure_project_dir
  cd "$PROJECT_DIR"
  if [ -f "$ENV_FILE" ]; then
    sudo_cmd docker compose --env-file "$ENV_FILE" "$@"
  else
    sudo_cmd docker compose "$@"
  fi
}

install_command() {
  ensure_project_dir
  wrapper='#!/usr/bin/env bash
set -euo pipefail
INSTALL_DIR="'"$PROJECT_DIR"'"
exec bash "$INSTALL_DIR/scripts/menu.sh" "$@"
'
  tmp_file="$(mktemp)"
  printf '%s' "$wrapper" > "$tmp_file"
  chmod +x "$tmp_file"
  sudo_cmd mv "$tmp_file" /usr/local/bin/shiye-bk
  ok "菜单命令已安装：shiye-bk"
}

install_project() {
  if [ -f "$PROJECT_DIR/scripts/deploy.sh" ]; then
    info "检测到已有项目，直接执行部署。"
    cd "$PROJECT_DIR"
    DEPLOY_AUTO_YES=1 bash scripts/deploy.sh
  else
    info "开始拉取并安装项目到 $PROJECT_DIR"
    tmp_file="$(mktemp)"
    curl -fsSL "https://raw.githubusercontent.com/wstimin/bokexitong/${BRANCH}/scripts/install.sh" -o "$tmp_file"
    INSTALL_DIR="$PROJECT_DIR" REPO_URL="$REPO_URL" BRANCH="$BRANCH" bash "$tmp_file"
    rm -f "$tmp_file"
  fi
  install_command
}

update_project() {
  ensure_project_dir
  cd "$PROJECT_DIR"
  if [ -d .git ]; then
    info "拉取最新代码..."
    git fetch origin "$BRANCH"
    git checkout "$BRANCH"
    git pull --ff-only origin "$BRANCH"
  else
    warn "当前目录不是 Git 仓库，跳过 git pull：$PROJECT_DIR"
  fi
  info "重新构建并启动服务..."
  DEPLOY_AUTO_YES=1 bash scripts/deploy.sh
  install_command
}

uninstall_project() {
  ensure_project_dir
  warn "卸载会停止并删除容器。选择彻底卸载会删除数据库、上传文件和项目目录。"
  if confirm "是否彻底卸载并删除数据？"; then
    bash "$PROJECT_DIR/scripts/uninstall.sh" --purge
  else
    bash "$PROJECT_DIR/scripts/uninstall.sh"
  fi
}

show_status() {
  ensure_project_dir
  domain="$(env_value BLOG_DOMAIN || true)"
  frontend_bind="$(env_value FRONTEND_HTTP_BIND || true)"
  admin_password="$(env_value BLOG_ADMIN_INITIAL_PASSWORD || true)"
  [ -n "$frontend_bind" ] || frontend_bind="80"

  printf '\n当前配置：\n'
  printf '  项目目录：%s\n' "$PROJECT_DIR"
  printf '  绑定域名：%s\n' "${domain:-未配置}"
  printf '  前端监听：%s\n' "$frontend_bind"
  if [ -n "$domain" ]; then
    printf '  反向代理：已配置域名后建议使用 Nginx / 面板反向代理到 127.0.0.1:18080\n'
  else
    printf '  反向代理：未配置\n'
  fi
  printf '\n后台账号：\n'
  printf '  用户名：admin\n'
  printf '  密码：%s\n' "${admin_password:-未读取到，请检查 .env}"

  printf '\n访问地址：\n'
  if [ -n "$domain" ]; then
    printf '  前台：https://%s/\n' "$domain"
    printf '  前台备用：http://%s/\n' "$domain"
    printf '  后台：https://%s/admin/login\n' "$domain"
    printf '  后台备用：http://%s/admin/login\n' "$domain"
  else
    printf '  前台：http://服务器IP/\n'
    printf '  后台：http://服务器IP/admin/login\n'
  fi
  case "$frontend_bind" in
    127.0.0.1:*) printf '  本机前端：http://127.0.0.1:%s/\n' "${frontend_bind#127.0.0.1:}" ;;
    *:*) printf '  端口访问：http://服务器IP:%s/\n' "${frontend_bind##*:}" ;;
    80) printf '  端口访问：http://服务器IP/\n' ;;
    *) printf '  端口访问：http://服务器IP:%s/\n' "$frontend_bind" ;;
  esac
  printf '\n'
  if has_cmd docker; then
    compose_cmd ps || true
  fi
}

install_nginx() {
  if has_cmd nginx; then
    return
  fi
  info "安装 Nginx..."
  if [ -f /etc/os-release ]; then
    . /etc/os-release
    os_id="${ID:-unknown}"
    os_like="${ID_LIKE:-}"
  else
    os_id="unknown"
    os_like=""
  fi
  if [[ "$os_id" =~ (ubuntu|debian) || "$os_like" =~ debian ]]; then
    sudo_cmd apt-get update
    sudo_cmd apt-get install -y nginx
  elif [[ "$os_id" =~ (centos|rocky|almalinux|rhel|fedora) || "$os_like" =~ (rhel|fedora) ]]; then
    if has_cmd dnf; then
      sudo_cmd dnf install -y nginx
    else
      sudo_cmd yum install -y nginx
    fi
  else
    fail "无法识别系统，请先手动安装 Nginx。"
  fi
  sudo_cmd systemctl enable nginx
  sudo_cmd systemctl start nginx
}

install_certbot() {
  if has_cmd certbot; then
    return
  fi
  info "安装 Certbot..."
  if [ -f /etc/os-release ]; then
    . /etc/os-release
    os_id="${ID:-unknown}"
    os_like="${ID_LIKE:-}"
  else
    os_id="unknown"
    os_like=""
  fi
  if [[ "$os_id" =~ (ubuntu|debian) || "$os_like" =~ debian ]]; then
    sudo_cmd apt-get update
    sudo_cmd apt-get install -y certbot python3-certbot-nginx
  elif [[ "$os_id" =~ (centos|rocky|almalinux|rhel|fedora) || "$os_like" =~ (rhel|fedora) ]]; then
    if has_cmd dnf; then
      sudo_cmd dnf install -y certbot python3-certbot-nginx
    else
      sudo_cmd yum install -y certbot python3-certbot-nginx
    fi
  else
    fail "无法识别系统，请先手动安装 Certbot。"
  fi
}

validate_domain() {
  domain="$1"
  [ -n "$domain" ] || fail "域名不能为空。"
  case "$domain" in
    http://*|https://*|*/*|*:*|*' '*) fail "只填写域名，不要带协议、端口或路径。" ;;
  esac
  printf '%s' "$domain" | grep -Eq '^[A-Za-z0-9.-]+$' || fail "域名格式不正确：$domain"
}

write_nginx_http_site() {
  domain="$1"
  install_nginx
  conf_path="/etc/nginx/conf.d/${NGINX_SITE_NAME}.conf"
  tmp_file="$(mktemp)"
  cat > "$tmp_file" <<EOF
server {
    listen 80;
    server_name ${domain};
    client_max_body_size 220m;

    location / {
        proxy_pass http://127.0.0.1:18080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF
  sudo_cmd mkdir -p /etc/nginx/conf.d
  sudo_cmd mv "$tmp_file" "$conf_path"
  sudo_cmd nginx -t
  sudo_cmd systemctl reload nginx
  ok "Nginx 域名反向代理已配置：$domain -> http://127.0.0.1:18080"
}

configure_domain() {
  ensure_project_dir
  current_domain="$(env_value BLOG_DOMAIN || true)"
  domain="${1:-}"
  if [ -z "$domain" ]; then
    [ -t 0 ] || fail "请提供域名，例如：shiye-bk domain example.com"
    domain="$(prompt '请输入要绑定的域名' "$current_domain")"
  fi
  validate_domain "$domain"
  set_env_value BLOG_DOMAIN "$domain"
  set_env_value FRONTEND_HTTP_BIND "127.0.0.1:18080"
  info "切换容器前端为本机监听端口 18080..."
  cd "$PROJECT_DIR"
  DEPLOY_AUTO_YES=1 bash scripts/deploy.sh
  write_nginx_http_site "$domain"
  ok "域名配置完成。解析生效后访问：http://$domain/"
}

issue_ssl() {
  ensure_project_dir
  domain="${1:-$(env_value BLOG_DOMAIN || true)}"
  if [ -z "$domain" ]; then
    [ -t 0 ] || fail "请先配置域名，例如：shiye-bk domain example.com"
    domain="$(prompt '请输入要申请证书的域名')"
    configure_domain "$domain"
  else
    validate_domain "$domain"
    write_nginx_http_site "$domain"
  fi
  email="${SSL_EMAIL:-}"
  if [ -z "$email" ]; then
    if [ -t 0 ]; then
      email="$(prompt '请输入证书通知邮箱，可留空跳过')"
    else
      email="admin@${domain}"
    fi
  fi
  install_certbot
  info "申请并启用 HTTPS 证书..."
  if [ -n "$email" ]; then
    sudo_cmd certbot --nginx -d "$domain" --non-interactive --agree-tos -m "$email" --redirect
  else
    sudo_cmd certbot --nginx -d "$domain" --non-interactive --agree-tos --register-unsafely-without-email --redirect
  fi
  set_env_value BLOG_DOMAIN "$domain"
  ok "证书已申请并启用：https://$domain/"
}

show_menu() {
  while true; do
    clear || true
    cat <<EOF
石页博客部署菜单
项目目录：$PROJECT_DIR

1. 安装 / 重新部署
2. 更新系统
3. 卸载系统
4. 查看当前配置
5. 配置域名
6. 申请并启用 SSL 证书
7. 查看容器状态
0. 退出
EOF
    printf '\n请选择操作: ' > /dev/tty
    read -r choice < /dev/tty || true
    case "$choice" in
      1) install_project; pause ;;
      2) update_project; pause ;;
      3) uninstall_project; pause ;;
      4) show_status; pause ;;
      5) configure_domain; pause ;;
      6) issue_ssl; pause ;;
      7) compose_cmd ps; pause ;;
      0) exit 0 ;;
      *) warn "无效选项：$choice"; pause ;;
    esac
  done
}

usage() {
  cat <<EOF
用法：shiye-bk [命令]

命令：
  menu                打开交互菜单
  install             安装或重新部署
  update              更新代码并重新部署
  uninstall           卸载系统
  status              查看当前配置、访问地址、用户名和密码
  domain <域名>       配置域名和 Nginx 反向代理
  ssl [域名]          申请并启用 HTTPS 证书
  help                查看帮助

不带命令时默认打开菜单。
EOF
}

main() {
  cmd="${1:-menu}"
  case "$cmd" in
    menu) show_menu ;;
    install) install_project ;;
    update) update_project ;;
    uninstall) uninstall_project ;;
    status|credentials) show_status ;;
    domain) shift || true; configure_domain "${1:-}" ;;
    ssl|cert) shift || true; issue_ssl "${1:-}" ;;
    help|-h|--help) usage ;;
    *) fail "未知命令：$cmd。执行 shiye-bk help 查看帮助。" ;;
  esac
}

main "$@"
