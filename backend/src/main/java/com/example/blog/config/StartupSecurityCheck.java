package com.example.blog.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.entity.BlogUser;
import com.example.blog.entity.SiteSetting;
import com.example.blog.mapper.BlogUserMapper;
import com.example.blog.mapper.SiteSettingMapper;
import com.example.blog.service.SiteSettingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class StartupSecurityCheck implements ApplicationRunner {
    private static final String DEFAULT_JWT_SECRET = "change-this-secret-to-a-long-random-value-for-production";
    private static final String COMPOSE_DEFAULT_JWT_SECRET = "please-change-this-secret-to-at-least-32-characters";

    private final Environment environment;
    private final SiteSettingMapper siteSettingMapper;
    private final BlogUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${blog.jwt.secret}")
    private String jwtSecret;

    @Value("${blog.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${blog.admin.initial-password:}")
    private String adminInitialPassword;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public StartupSecurityCheck(Environment environment, SiteSettingMapper siteSettingMapper,
                                BlogUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.environment = environment;
        this.siteSettingMapper = siteSettingMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProd() || !installationComplete()) {
            return;
        }
        if (DEFAULT_JWT_SECRET.equals(jwtSecret) || COMPOSE_DEFAULT_JWT_SECRET.equals(jwtSecret) || jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("生产环境必须配置长度不少于 32 位的 BLOG_JWT_SECRET");
        }
        if (mailEnabled && (blank(mailHost) || blank(mailUsername) || blank(mailPassword))) {
            throw new IllegalStateException("已开启邮件服务，但 SMTP 主机、账号或密码未配置完整");
        }
        BlogUser admin = userMapper.selectOne(new LambdaQueryWrapper<BlogUser>()
                .eq(BlogUser::getUsername, "admin")
                .last("LIMIT 1"));
        if (admin != null && passwordEncoder.matches("123456", admin.getPassword())) {
            if (blank(adminInitialPassword)) {
                throw new IllegalStateException("生产环境禁止使用默认管理员密码，请配置 BLOG_ADMIN_INITIAL_PASSWORD 或先修改 admin 密码");
            }
            String password = adminInitialPassword.trim();
            if (password.length() < 8) {
                throw new IllegalStateException("BLOG_ADMIN_INITIAL_PASSWORD 长度不能少于 8 位");
            }
            admin.setPassword(passwordEncoder.encode(password));
            admin.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(admin);
        }
    }

    private boolean isProd() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private boolean installationComplete() {
        try {
            SiteSetting row = siteSettingMapper.selectOne(new LambdaQueryWrapper<SiteSetting>()
                    .eq(SiteSetting::getSettingKey, SiteSettingService.INSTALLATION_COMPLETE)
                    .last("LIMIT 1"));
            return row != null && "true".equalsIgnoreCase(String.valueOf(row.getSettingValue()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
