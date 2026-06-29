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

@Service
public class AuthService {
    private final BlogUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(BlogUserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        BlogUser user = userMapper.selectOne(new LambdaQueryWrapper<BlogUser>()
                .eq(BlogUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        user.setPassword(null);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user);
    }

    public BlogUser register(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<BlogUser>()
                .eq(BlogUser::getUsername, request.getUsername()));
        if (exists > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        BlogUser user = new BlogUser();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname() == null ? request.getUsername() : request.getNickname());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
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
        user.setNickname(request.getNickname());
        user.setAvatar(request.getAvatar());
        user.setEmail(request.getEmail());
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
        user.setNickname(request.getNickname());
        user.setAvatar(request.getAvatar());
        user.setEmail(request.getEmail());
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

    private void updatePassword(BlogUser user, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少 6 位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
