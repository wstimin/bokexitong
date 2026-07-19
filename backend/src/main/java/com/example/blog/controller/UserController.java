package com.example.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.common.Result;
import com.example.blog.dto.ArticleCardResponse;
import com.example.blog.dto.PasswordChangeRequest;
import com.example.blog.dto.ProfileRequest;
import com.example.blog.dto.UserCommentResponse;
import com.example.blog.entity.BlogUser;
import com.example.blog.security.BlogPrincipal;
import com.example.blog.service.ArticleService;
import com.example.blog.service.AuthService;
import com.example.blog.service.InteractionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final AuthService authService;
    private final ArticleService articleService;
    private final InteractionService interactionService;

    public UserController(AuthService authService, ArticleService articleService, InteractionService interactionService) {
        this.authService = authService;
        this.articleService = articleService;
        this.interactionService = interactionService;
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

    @GetMapping("/favorites")
    public Result<Page<ArticleCardResponse>> favorites(@AuthenticationPrincipal BlogPrincipal principal,
                                                       @RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "10") long size) {
        return Result.ok(articleService.favoritePage(principal.userId(), current, size));
    }

    @GetMapping("/comments")
    public Result<Page<UserCommentResponse>> comments(@AuthenticationPrincipal BlogPrincipal principal,
                                                      @RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size) {
        return Result.ok(interactionService.userComments(principal.userId(), current, size));
    }
}
