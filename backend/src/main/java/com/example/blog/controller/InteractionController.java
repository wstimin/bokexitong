package com.example.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.common.Result;
import com.example.blog.dto.InteractionResponse;
import com.example.blog.entity.Comment;
import com.example.blog.security.BlogPrincipal;
import com.example.blog.service.InteractionService;
import com.example.blog.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api")
public class InteractionController {
    private final InteractionService interactionService;
    private final RateLimitService rateLimitService;

    public InteractionController(InteractionService interactionService, RateLimitService rateLimitService) {
        this.interactionService = interactionService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/comments")
    public Result<Page<Comment>> comments(@RequestParam Long articleId,
                                          @RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.ok(interactionService.comments(articleId, current, size));
    }

    @PostMapping("/comments")
    public Result<Comment> comment(HttpServletRequest request, @AuthenticationPrincipal BlogPrincipal principal, @RequestBody Comment comment) {
        rateLimitService.check("comment:user:" + principal.userId(), 8, Duration.ofMinutes(10));
        rateLimitService.check("comment:ip:" + clientIp(request), 20, Duration.ofMinutes(10));
        return Result.ok(interactionService.comment(principal.userId(), comment));
    }

    @PostMapping("/articles/{id}/like")
    public Result<InteractionResponse> like(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id) {
        rateLimitService.check("like:user:" + principal.userId(), 60, Duration.ofMinutes(10));
        return Result.ok(interactionService.like(principal.userId(), id));
    }

    @PostMapping("/articles/{id}/favorite")
    public Result<InteractionResponse> favorite(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id) {
        rateLimitService.check("favorite:user:" + principal.userId(), 60, Duration.ofMinutes(10));
        return Result.ok(interactionService.favorite(principal.userId(), id));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
