package com.example.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.dto.AdminUserRequest;
import com.example.blog.dto.LoginRequest;
import com.example.blog.dto.LoginResponse;
import com.example.blog.dto.PasswordChangeRequest;
import com.example.blog.dto.PasswordResetRequest;
import com.example.blog.dto.ProfileRequest;
import com.example.blog.dto.RegisterRequest;
import com.example.blog.entity.BlogUser;
import com.example.blog.mapper.BlogUserMapper;
import com.example.blog.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    private final BlogUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailCodeService emailCodeService;
    private final SiteSettingService siteSettingService;

    public AuthService(BlogUserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
                       EmailCodeService emailCodeService, SiteSettingService siteSettingService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailCodeService = emailCodeService;
        this.siteSettingService = siteSettingService;
    }

    public LoginResponse login(LoginRequest request) {
        String account = clean(request.getAccount() == null ? request.getUsername() : request.getAccount());
        if (account == null || clean(request.getPassword()) == null) {
            throw new IllegalArgumentException("请填写账号和密码");
        }
        BlogUser user = userMapper.selectOne(new LambdaQueryWrapper<BlogUser>()
                .eq(BlogUser::getUsername, account)
                .or()
                .eq(BlogUser::getEmail, account));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        boolean passwordChangeRequired = isDefaultAdminPassword(user);
        user.setPassword(null);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user, passwordChangeRequired);
    }

    public BlogUser register(RegisterRequest request) {
        if (!siteSettingService.allowRegister()) {
            throw new IllegalArgumentException("站点暂未开放注册");
        }
        String username = clean(request.getUsername());
        String email = clean(request.getEmail());
        String password = clean(request.getPassword());
        String nickname = clean(request.getNickname());

        validateUsername(username);
        validateEmail(email);
        validatePassword(password);
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<BlogUser>()
                .eq(BlogUser::getUsername, username));
        if (exists > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        ensureEmailUnique(email, null);
        emailCodeService.verifyAndUse(email, request.getCode(), EmailCodeService.SCENE_REGISTER);
        BlogUser user = new BlogUser();
        user.setUsername(username);
        user.setNickname(nickname == null ? username : nickname);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole("USER");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    public BlogUser currentUser(Long userId) {
        BlogUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    public BlogUser updateProfile(Long userId, ProfileRequest request) {
        BlogUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String email = clean(request.getEmail());
        validateEmail(email);
        ensureEmailUnique(email, userId);
        user.setNickname(defaultIfBlank(request.getNickname(), user.getUsername()));
        user.setAvatar(clean(request.getAvatar()));
        user.setEmail(email);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    public void changePassword(Long userId, PasswordChangeRequest request) {
        BlogUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("原密码不正确");
        }
        updatePassword(user, request.getNewPassword());
    }

    public BlogUser updateUserByAdmin(Long id, AdminUserRequest request) {
        BlogUser user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String email = clean(request.getEmail());
        validateEmail(email);
        ensureEmailUnique(email, id);
        user.setNickname(defaultIfBlank(request.getNickname(), user.getUsername()));
        user.setAvatar(clean(request.getAvatar()));
        user.setEmail(email);
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    public void resetPasswordByAdmin(Long id, PasswordResetRequest request) {
        BlogUser user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        updatePassword(user, request.getNewPassword());
    }

    public void resetPasswordByEmail(String emailValue, String code, String newPassword) {
        String email = clean(emailValue);
        String password = clean(newPassword);
        validateEmail(email);
        validatePassword(password);
        BlogUser user = userMapper.selectOne(new LambdaQueryWrapper<BlogUser>()
                .eq(BlogUser::getEmail, email)
                .last("LIMIT 1"));
        if (user == null) {
            throw new IllegalArgumentException("该邮箱未注册账号");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用，请联系管理员");
        }
        emailCodeService.verifyAndUse(email, code, EmailCodeService.SCENE_RESET_PASSWORD);
        updatePassword(user, password);
    }

    private void updatePassword(BlogUser user, String newPassword) {
        String password = clean(newPassword);
        validatePassword(password);
        user.setPassword(passwordEncoder.encode(password));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private void validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("用户名需为 3-20 位字母、数字或下划线");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("请填写正确的邮箱地址");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
    }

    private void ensureEmailUnique(String email, Long currentUserId) {
        BlogUser exists = userMapper.selectOne(new LambdaQueryWrapper<BlogUser>()
                .eq(BlogUser::getEmail, email)
                .last("LIMIT 1"));
        if (exists != null && (currentUserId == null || !exists.getId().equals(currentUserId))) {
            throw new IllegalArgumentException("邮箱已被使用");
        }
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        String trimmed = clean(value);
        return trimmed == null ? fallback : trimmed;
    }

    private boolean isDefaultAdminPassword(BlogUser user) {
        return "ADMIN".equals(user.getRole())
                && "admin".equals(user.getUsername())
                && passwordEncoder.matches("123456", user.getPassword());
    }
}
