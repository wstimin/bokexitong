package com.example.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.common.Result;
import com.example.blog.entity.Comment;
import com.example.blog.security.BlogPrincipal;
import com.example.blog.service.InteractionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class InteractionController {
    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @GetMapping("/comments")
    public Result<Page<Comment>> comments(@RequestParam Long articleId,
                                          @RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.ok(interactionService.comments(articleId, current, size));
    }

    @PostMapping("/comments")
    public Result<Comment> comment(@AuthenticationPrincipal BlogPrincipal principal, @RequestBody Comment comment) {
        return Result.ok(interactionService.comment(principal.userId(), comment));
    }

    @PostMapping("/articles/{id}/like")
    public Result<Void> like(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id) {
        interactionService.like(principal.userId(), id);
        return Result.ok();
    }

    @PostMapping("/articles/{id}/favorite")
    public Result<Void> favorite(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id) {
        interactionService.favorite(principal.userId(), id);
        return Result.ok();
    }
}
