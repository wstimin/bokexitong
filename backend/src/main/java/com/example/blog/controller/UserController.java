package com.example.blog.controller;

import com.example.blog.common.Result;
import com.example.blog.dto.PasswordChangeRequest;
import com.example.blog.dto.ProfileRequest;
import com.example.blog.entity.BlogUser;
import com.example.blog.security.BlogPrincipal;
import com.example.blog.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
public class UserController {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public Result<BlogUser> me(@AuthenticationPrincipal BlogPrincipal principal) {
        return Result.ok(authService.currentUser(principal.userId()));
    }

    @PutMapping
    public Result<BlogUser> updateProfile(@AuthenticationPrincipal BlogPrincipal principal,
                                          @RequestBody ProfileRequest request) {
        return Result.ok(authService.updateProfile(principal.userId(), request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal BlogPrincipal principal,
                                       @RequestBody PasswordChangeRequest request) {
        authService.changePassword(principal.userId(), request);
        return Result.ok();
    }
}
