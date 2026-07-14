package com.example.blog.controller;

import com.example.blog.common.Result;
import com.example.blog.dto.EmailCodeRequest;
import com.example.blog.dto.LoginRequest;
import com.example.blog.dto.LoginResponse;
import com.example.blog.dto.PasswordResetByEmailRequest;
import com.example.blog.dto.RegisterRequest;
import com.example.blog.entity.BlogUser;
import com.example.blog.service.AuthService;
import com.example.blog.service.EmailCodeService;
import com.example.blog.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final EmailCodeService emailCodeService;
    private final RateLimitService rateLimitService;

    public AuthController(AuthService authService, EmailCodeService emailCodeService, RateLimitService rateLimitService) {
        this.authService = authService;
        this.emailCodeService = emailCodeService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(HttpServletRequest httpRequest, @RequestBody LoginRequest request) {
        rateLimitService.check("login:" + clientIp(httpRequest), 10, java.time.Duration.ofMinutes(5));
        return Result.ok(authService.login(request));
    }

    @PostMapping("/register")
    public Result<BlogUser> register(HttpServletRequest httpRequest, @RequestBody RegisterRequest request) {
        rateLimitService.check("register:" + clientIp(httpRequest), 5, java.time.Duration.ofMinutes(10));
        return Result.ok(authService.register(request));
    }

    @PostMapping("/email-code")
    public Result<Void> emailCode(HttpServletRequest httpRequest, @RequestBody EmailCodeRequest request) {
        rateLimitService.check("email-code:ip:" + clientIp(httpRequest), 8, java.time.Duration.ofMinutes(10));
        rateLimitService.check("email-code:mail:" + String.valueOf(request.getEmail()).trim().toLowerCase(), 3, java.time.Duration.ofMinutes(10));
        emailCodeService.sendCode(request.getEmail(), request.getScene());
        return Result.ok();
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(HttpServletRequest httpRequest, @RequestBody PasswordResetByEmailRequest request) {
        rateLimitService.check("reset-password:" + clientIp(httpRequest), 5, java.time.Duration.ofMinutes(10));
        authService.resetPasswordByEmail(request.getEmail(), request.getCode(), request.getNewPassword());
        return Result.ok();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
