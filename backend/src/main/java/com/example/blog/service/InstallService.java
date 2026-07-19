package com.example.blog.service;

import com.example.blog.dto.InstallRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;

@Service
public class InstallService {
    private static final String DEFAULT_SITE_NAME = "博客系统";
    private static final String DEFAULT_DB_HOST = "127.0.0.1";
    private static final String DEFAULT_DB_PORT = "3306";
    private static final String DEFAULT_DB_NAME = "personal_blog";
    private static final String DEFAULT_DB_USERNAME = "root";
    private static final String JDBC_OPTIONS = "useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=5000";

    private final PasswordEncoder passwordEncoder;
    private final Path runtimeConfigPath;
    private final boolean restartAfterInstall;
    private final SecureRandom secureRandom = new SecureRandom();

    public InstallService(PasswordEncoder passwordEncoder,
                          @Value("${blog.install.runtime-config-path:./config/bokexitong-runtime.properties}") String runtimeConfigPath,
                          @Value("${blog.install.restart-after-install:false}") boolean restartAfterInstall) {
        this.passwordEncoder = passwordEncoder;
        this.runtimeConfigPath = Path.of(runtimeConfigPath);
        this.restartAfterInstall = restartAfterInstall;
    }

    public boolean hasRuntimeConfig() {
        return Files.isRegularFile(runtimeConfigPath);
    }

    public boolean isRestartAfterInstallEnabled() {
        return restartAfterInstall;
    }

    public InstallTarget normalize(InstallRequest request) {
        InstallRequest source = request == null ? new InstallRequest() : request;
        String host = fallback(source.getDbHost(), DEFAULT_DB_HOST);
        String port = fallback(source.getDbPort(), DEFAULT_DB_PORT);
        String name = fallback(source.getDbName(), DEFAULT_DB_NAME);
        String username = fallback(source.getDbUsername(), DEFAULT_DB_USERNAME);
        String password = source.getDbPassword();
        return new InstallTarget(host, port, name, username, password == null ? "" : password.trim());
    }

    public void install(InstallRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请完整填写安装信息");
        }
        if (isBlank(request.getAdminPassword())) {
            throw new IllegalArgumentException("请填写管理员密码");
        }
        if (request.getAdminPassword().trim().length() < 8) {
            throw new IllegalArgumentException("管理员密码至少 8 位");
        }

        InstallTarget target = normalize(request);
        validateTarget(target);
        ensureDatabase(target);
        try (Connection connection = DriverManager.getConnection(jdbcUrl(target), target.username(), target.password())) {
            initializeSchema(connection);
            initializeData(connection, request);
        } catch (SQLException ex) {
            throw new IllegalArgumentException("数据库连接或初始化失败：" + friendlySqlMessage(ex));
        }
        writeRuntimeConfig(target, request);
    }

    public void restartSoon() {
        if (!restartAfterInstall) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            System.exit(0);
        }, "bokexitong-install-restart");
        thread.setDaemon(false);
        thread.start();
    }

    private void validateTarget(InstallTarget target) {
        if (!target.port().matches("\\d{1,5}")) {
            throw new IllegalArgumentException("数据库端口格式不正确");
        }
        int portValue = Integer.parseInt(target.port());
        if (portValue < 1 || portValue > 65535) {
            throw new IllegalArgumentException("数据库端口范围不正确");
        }
        if (!target.name().matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("数据库名只能包含字母、数字和下划线");
        }
        if (isBlank(target.username())) {
            throw new IllegalArgumentException("请填写数据库用户名");
        }
    }

    private void ensureDatabase(InstallTarget target) {
        try (Connection connection = DriverManager.getConnection(serverJdbcUrl(target), target.username(), target.password());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + target.name() + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException createFailed) {
            try (Connection ignored = DriverManager.getConnection(jdbcUrl(target), target.username(), target.password())) {
                return;
            } catch (SQLException connectFailed) {
                throw new IllegalArgumentException("无法连接数据库或创建数据库：" + friendlySqlMessage(connectFailed));
            }
        }
    }

    private void initializeSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : schemaSql()) {
                statement.executeUpdate(sql);
            }
        }
    }

    private void initializeData(Connection connection, InstallRequest request) throws SQLException {
        upsertAdmin(connection, request.getAdminPassword().trim());
        String siteName = fallback(request.getSiteName(), DEFAULT_SITE_NAME);
        upsertSetting(connection, "siteName", siteName, "站点名称");
        upsertSetting(connection, "heroTitle", siteName, "首页大标题");
        upsertSetting(connection, "heroSubtitle", "用卡片浏览公开文章，点开后再阅读完整内容。登录后可以进入用户中心创作、管理自己的文章。", "首页说明");
        upsertSetting(connection, "heroBadge", "博客", "首页徽标文字");
        upsertSetting(connection, "backgroundUrl", "", "全站背景图 URL");
        upsertSetting(connection, "logoUrl", "", "站点 Logo 和浏览器图标 URL");
        upsertSetting(connection, "seoDescription", "", "SEO 描述");
        upsertSetting(connection, "seoKeywords", "", "SEO 关键词");
        upsertSetting(connection, "icpBeian", "", "备案号");
        upsertSetting(connection, "footerText", "", "页脚文字");
        upsertSetting(connection, "contactHtml", "", "前台联系信息 HTML");
        upsertSetting(connection, "allowRegister", "true", "是否开放公开注册");
        upsertSetting(connection, "adminLoginPath", "/admin/login", "后台登录路径");
        upsertSetting(connection, "forbiddenWords", "赌博 色情 毒品 诈骗", "文章违禁词");
        upsertSetting(connection, "mailEnabled", "false", "是否启用后台 SMTP 邮件服务");
        upsertSetting(connection, "mailHost", "", "SMTP 服务器地址");
        upsertSetting(connection, "mailPort", "587", "SMTP 端口");
        upsertSetting(connection, "mailUsername", "", "SMTP 登录账号");
        upsertSetting(connection, "mailPassword", "", "SMTP 登录密码或授权码");
        upsertSetting(connection, "mailFromName", siteName, "邮件发件名称");
        upsertSetting(connection, "mailSmtpAuth", "true", "SMTP 是否需要认证");
        upsertSetting(connection, "mailStarttlsEnable", "true", "SMTP 是否启用 STARTTLS");
        upsertSetting(connection, "mailSslEnable", "false", "SMTP 是否启用 SSL");
        if (!isBlank(request.getDomain())) {
            upsertSetting(connection, "blogDomain", request.getDomain().trim(), "访问域名");
        }
        upsertSetting(connection, "installationComplete", "true", "安装完成标记");
    }

    private void upsertAdmin(Connection connection, String password) throws SQLException {
        Long id = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM blog_user WHERE username = ? LIMIT 1")) {
            statement.setString(1, "admin");
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    id = resultSet.getLong("id");
                }
            }
        }
        String encoded = passwordEncoder.encode(password);
        if (id == null) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO blog_user(username, nickname, password, avatar, email, role, status, created_at, updated_at) VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?)")) {
                statement.setString(1, "admin");
                statement.setString(2, "站长");
                statement.setString(3, encoded);
                statement.setString(4, "");
                statement.setString(5, "ADMIN");
                statement.setInt(6, 1);
                statement.setObject(7, LocalDateTime.now());
                statement.setObject(8, LocalDateTime.now());
                statement.executeUpdate();
            }
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("UPDATE blog_user SET password = ?, role = ?, status = ?, updated_at = ? WHERE id = ?")) {
            statement.setString(1, encoded);
            statement.setString(2, "ADMIN");
            statement.setInt(3, 1);
            statement.setObject(4, LocalDateTime.now());
            statement.setLong(5, id);
            statement.executeUpdate();
        }
    }

    private void upsertSetting(Connection connection, String key, String value, String description) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO site_setting(setting_key, setting_value, description, updated_at) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), description = VALUES(description), updated_at = VALUES(updated_at)")) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.setString(3, description);
            statement.setObject(4, LocalDateTime.now());
            statement.executeUpdate();
        }
    }

    private void writeRuntimeConfig(InstallTarget target, InstallRequest request) {
        Properties properties = new Properties();
        properties.setProperty("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
        properties.setProperty("spring.datasource.url", jdbcUrl(target));
        properties.setProperty("spring.datasource.username", target.username());
        properties.setProperty("spring.datasource.password", target.password());
        properties.setProperty("blog.jwt.secret", validJwtSecret(request.getJwtSecret()));
        Path targetPath = runtimeConfigPath.toAbsolutePath().normalize();
        Path temporaryPath = null;
        try {
            Path parent = targetPath.getParent();
            if (parent == null) {
                throw new IOException("配置文件目录无效");
            }
            Files.createDirectories(parent);
            temporaryPath = Files.createTempFile(parent, "bokexitong-runtime-", ".properties.tmp");
            try (OutputStream outputStream = Files.newOutputStream(temporaryPath)) {
                properties.store(outputStream, "Generated by web install wizard");
            }
            try {
                Files.move(temporaryPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            if (temporaryPath != null) {
                try {
                    Files.deleteIfExists(temporaryPath);
                } catch (IOException ignored) {
                }
            }
            throw new IllegalArgumentException("运行配置写入失败：" + ex.getMessage());
        }
    }

    private String validJwtSecret(String value) {
        if (!isBlank(value) && value.trim().length() >= 32) {
            return value.trim();
        }
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String jdbcUrl(InstallTarget target) {
        return "jdbc:mysql://" + target.host() + ":" + target.port() + "/" + target.name() + "?" + JDBC_OPTIONS;
    }

    private String serverJdbcUrl(InstallTarget target) {
        return "jdbc:mysql://" + target.host() + ":" + target.port() + "/?" + JDBC_OPTIONS;
    }

    private String friendlySqlMessage(SQLException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private String fallback(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private List<String> schemaSql() {
        return List.of(
                "CREATE TABLE IF NOT EXISTS blog_user (id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50) NOT NULL UNIQUE, nickname VARCHAR(80) NOT NULL, password VARCHAR(120) NOT NULL, avatar VARCHAR(500), email VARCHAR(120) UNIQUE, role VARCHAR(20) NOT NULL DEFAULT 'USER', status TINYINT NOT NULL DEFAULT 1, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS category (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(80) NOT NULL, description VARCHAR(255), sort INT DEFAULT 0, created_at DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS tag (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(80) NOT NULL, color VARCHAR(20) DEFAULT '#ff77b7', created_at DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS article (id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, category_id BIGINT, title VARCHAR(180) NOT NULL, summary VARCHAR(500), cover_url VARCHAR(800), content LONGTEXT, content_type VARCHAR(20) DEFAULT 'MARKDOWN', status VARCHAR(20) DEFAULT 'DRAFT', recommended TINYINT DEFAULT 0, recommend_sort INT DEFAULT 0, view_count INT DEFAULT 0, like_count INT DEFAULT 0, favorite_count INT DEFAULT 0, published_at DATETIME, review_reason VARCHAR(500), reviewed_at DATETIME, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted TINYINT DEFAULT 0, INDEX idx_article_status(status), INDEX idx_article_recommend(recommended, recommend_sort), INDEX idx_article_category(category_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS article_tag (id BIGINT PRIMARY KEY AUTO_INCREMENT, article_id BIGINT NOT NULL, tag_id BIGINT NOT NULL, UNIQUE KEY uk_article_tag(article_id, tag_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS comment (id BIGINT PRIMARY KEY AUTO_INCREMENT, article_id BIGINT NOT NULL, user_id BIGINT NOT NULL, parent_id BIGINT DEFAULT 0, content VARCHAR(1000) NOT NULL, status VARCHAR(20) DEFAULT 'PENDING', review_reason VARCHAR(500), reviewed_at DATETIME, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, INDEX idx_comment_article(article_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS like_record (id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, article_id BIGINT NOT NULL, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uk_like(user_id, article_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS favorite (id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, article_id BIGINT NOT NULL, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uk_favorite(user_id, article_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS image_resource (id BIGINT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(120) NOT NULL, url VARCHAR(800) NOT NULL, type VARCHAR(30) NOT NULL COMMENT 'LOGO/HERO/BACKGROUND/COVER/AVATAR/RECOMMEND', description VARCHAR(255), sort INT DEFAULT 0, enabled TINYINT DEFAULT 1, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, INDEX idx_image_type(type, enabled)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS site_setting (id BIGINT PRIMARY KEY AUTO_INCREMENT, setting_key VARCHAR(80) NOT NULL UNIQUE, setting_value TEXT, description VARCHAR(255), updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS operation_log (id BIGINT PRIMARY KEY AUTO_INCREMENT, operator_id BIGINT, operator_name VARCHAR(80), action VARCHAR(40) NOT NULL, target_type VARCHAR(40) NOT NULL, target_id BIGINT, detail VARCHAR(1000), created_at DATETIME DEFAULT CURRENT_TIMESTAMP, INDEX idx_operation_log_created(created_at), INDEX idx_operation_log_action(action), INDEX idx_operation_log_target(target_type, target_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS email_code (id BIGINT PRIMARY KEY AUTO_INCREMENT, email VARCHAR(120) NOT NULL, code_hash VARCHAR(120) NOT NULL, scene VARCHAR(40) NOT NULL COMMENT 'REGISTER/RESET_PASSWORD', used TINYINT NOT NULL DEFAULT 0, expires_at DATETIME NOT NULL, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, used_at DATETIME, INDEX idx_email_code_lookup(email, scene, used, expires_at), INDEX idx_email_code_created(created_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    }

    public record InstallTarget(String host, String port, String name, String username, String password) {
    }
}
