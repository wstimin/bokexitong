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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
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
        JavaMailSender sender = requireMailSender();

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
            sendMail(sender, email, scene, code);
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

    private JavaMailSender requireMailSender() {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || sender == null || clean(mailFrom) == null || clean(mailHost) == null) {
            throw new IllegalArgumentException("邮件服务未配置，无法发送验证码");
        }
        return sender;
    }

    private void sendMail(JavaMailSender sender, String email, String scene, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject(subject(scene));
        message.setText("【" + fromName + "】您的验证码是：" + code + "，10 分钟内有效。如非本人操作，请忽略本邮件。");
        sender.send(message);
    }

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
