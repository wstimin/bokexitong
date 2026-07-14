package com.example.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.entity.BlogUser;
import com.example.blog.entity.EmailCode;
import com.example.blog.mapper.BlogUserMapper;
import com.example.blog.mapper.EmailCodeMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class EmailCodeService {
    public static final String SCENE_REGISTER = "REGISTER";
    public static final String SCENE_RESET_PASSWORD = "RESET_PASSWORD";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Set<String> SCENES = Set.of(SCENE_REGISTER, SCENE_RESET_PASSWORD);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailCodeMapper emailCodeMapper;
    private final BlogUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final SiteSettingService siteSettingService;

    @Value("${blog.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}")
    private boolean sslEnable;

    @Value("${blog.mail.from-name:博客系统}")
    private String fromName;

    public EmailCodeService(EmailCodeMapper emailCodeMapper, BlogUserMapper userMapper,
                            PasswordEncoder passwordEncoder, ObjectProvider<JavaMailSender> mailSenderProvider,
                            SiteSettingService siteSettingService) {
        this.emailCodeMapper = emailCodeMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.mailSenderProvider = mailSenderProvider;
        this.siteSettingService = siteSettingService;
    }

    public void sendCode(String rawEmail, String rawScene) {
        String email = clean(rawEmail);
        String scene = normalizeScene(rawScene);
        validateEmail(email);
        if (SCENE_REGISTER.equals(scene) && !siteSettingService.allowRegister()) {
            throw new IllegalArgumentException("站点暂未开放注册");
        }
        validateSceneTarget(email, scene);
        enforceSendInterval(email, scene);
        MailConfig mailConfig = requireMailConfig();
        JavaMailSender sender = mailConfig.sender();

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();
        EmailCode row = new EmailCode();
        row.setEmail(email);
        row.setScene(scene);
        row.setCodeHash(passwordEncoder.encode(code));
        row.setUsed(0);
        row.setExpiresAt(now.plusMinutes(10));
        row.setCreatedAt(now);
        emailCodeMapper.insert(row);

        try {
            sendMail(sender, mailConfig, email, scene, code);
        } catch (RuntimeException ex) {
            row.setUsed(1);
            row.setUsedAt(LocalDateTime.now());
            emailCodeMapper.updateById(row);
            throw new IllegalArgumentException("验证码邮件发送失败，请稍后再试");
        }
    }

    public void verifyAndUse(String rawEmail, String rawCode, String rawScene) {
        String email = clean(rawEmail);
        String code = clean(rawCode);
        String scene = normalizeScene(rawScene);
        validateEmail(email);
        if (code == null || !code.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("请填写 6 位邮箱验证码");
        }
        EmailCode row = emailCodeMapper.selectOne(new LambdaQueryWrapper<EmailCode>()
                .eq(EmailCode::getEmail, email)
                .eq(EmailCode::getScene, scene)
                .eq(EmailCode::getUsed, 0)
                .ge(EmailCode::getExpiresAt, LocalDateTime.now())
                .orderByDesc(EmailCode::getCreatedAt)
                .last("LIMIT 1"));
        if (row == null || !passwordEncoder.matches(code, row.getCodeHash())) {
            throw new IllegalArgumentException("邮箱验证码不正确或已过期");
        }
        row.setUsed(1);
        row.setUsedAt(LocalDateTime.now());
        emailCodeMapper.updateById(row);
    }

    public void sendTestMail(String rawEmail) {
        String email = clean(rawEmail);
        validateEmail(email);
        MailConfig mailConfig = requireMailConfig();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailConfig.from());
        message.setTo(email);
        message.setSubject("邮箱配置测试");
        message.setText("【" + mailConfig.fromName() + "】这是一封测试邮件。收到这封邮件说明后台 SMTP 配置已经生效。");
        mailConfig.sender().send(message);
    }

    private void validateSceneTarget(String email, String scene) {
        BlogUser user = userMapper.selectOne(new LambdaQueryWrapper<BlogUser>()
                .eq(BlogUser::getEmail, email)
                .last("LIMIT 1"));
        if (SCENE_REGISTER.equals(scene) && user != null) {
            throw new IllegalArgumentException("邮箱已被使用");
        }
        if (SCENE_RESET_PASSWORD.equals(scene) && user == null) {
            throw new IllegalArgumentException("该邮箱未注册账号");
        }
        if (SCENE_RESET_PASSWORD.equals(scene) && user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用，请联系管理员");
        }
    }

    private void enforceSendInterval(String email, String scene) {
        EmailCode latest = emailCodeMapper.selectOne(new LambdaQueryWrapper<EmailCode>()
                .eq(EmailCode::getEmail, email)
                .eq(EmailCode::getScene, scene)
                .orderByDesc(EmailCode::getCreatedAt)
                .last("LIMIT 1"));
        if (latest != null && latest.getCreatedAt() != null && latest.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
            throw new IllegalArgumentException("验证码发送过于频繁，请稍后再试");
        }
    }

    private MailConfig requireMailConfig() {
        Map<String, String> settings = siteSettingService.mailSettings();
        boolean dbEnabled = bool(settings.get(SiteSettingService.MAIL_ENABLED), false);
        String dbHost = clean(settings.get(SiteSettingService.MAIL_HOST));
        String dbUsername = clean(settings.get(SiteSettingService.MAIL_USERNAME));
        String dbPassword = clean(settings.get(SiteSettingService.MAIL_PASSWORD));

        if (dbEnabled) {
            if (dbHost == null || dbUsername == null || dbPassword == null) {
                throw new IllegalArgumentException("邮件服务未配置完整，无法发送验证码");
            }
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(dbHost);
            sender.setPort(parsePort(settings.get(SiteSettingService.MAIL_PORT), 587));
            sender.setUsername(dbUsername);
            sender.setPassword(dbPassword);
            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", String.valueOf(bool(settings.get(SiteSettingService.MAIL_SMTP_AUTH), true)));
            props.put("mail.smtp.starttls.enable", String.valueOf(bool(settings.get(SiteSettingService.MAIL_STARTTLS_ENABLE), true)));
            props.put("mail.smtp.ssl.enable", String.valueOf(bool(settings.get(SiteSettingService.MAIL_SSL_ENABLE), false)));
            return new MailConfig(sender, dbUsername, clean(settings.get(SiteSettingService.MAIL_FROM_NAME), fromName));
        }

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || sender == null || clean(mailFrom) == null || clean(mailHost) == null) {
            throw new IllegalArgumentException("邮件服务未配置，无法发送验证码");
        }
        return new MailConfig(sender, mailFrom, fromName);
    }

    private void sendMail(JavaMailSender sender, MailConfig mailConfig, String email, String scene, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailConfig.from());
        message.setTo(email);
        message.setSubject(subject(scene));
        message.setText("【" + mailConfig.fromName() + "】您的验证码是：" + code + "，10 分钟内有效。如非本人操作，请忽略本邮件。");
        sender.send(message);
    }

    private boolean bool(String value, boolean fallback) {
        String trimmed = clean(value);
        return trimmed == null ? fallback : "true".equalsIgnoreCase(trimmed);
    }

    private int parsePort(String value, int fallback) {
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String clean(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned == null ? fallback : cleaned;
    }

    private record MailConfig(JavaMailSender sender, String from, String fromName) {}

    private String subject(String scene) {
        return SCENE_RESET_PASSWORD.equals(scene) ? "重置密码验证码" : "注册账号验证码";
    }

    private String normalizeScene(String scene) {
        String value = clean(scene);
        if (value == null) {
            value = SCENE_REGISTER;
        }
        value = value.toUpperCase();
        if (!SCENES.contains(value)) {
            throw new IllegalArgumentException("验证码场景不正确");
        }
        return value;
    }

    private void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("请填写正确的邮箱地址");
        }
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
