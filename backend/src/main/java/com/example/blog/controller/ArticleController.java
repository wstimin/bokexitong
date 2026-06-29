package com.example.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.common.Result;
import com.example.blog.dto.ArticleRequest;
import com.example.blog.entity.Article;
import com.example.blog.security.BlogPrincipal;
import com.example.blog.service.ArticleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/articles")
public class ArticleController {
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<Article>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) Long categoryId) {
        return Result.ok(articleService.page(current, size, keyword, status, categoryId));
    }

    @GetMapping("/mine")
    public Result<Page<Article>> mine(@AuthenticationPrincipal BlogPrincipal principal,
                                      @RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String status) {
        return Result.ok(articleService.userPage(principal.userId(), current, size, keyword, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Article> detail(@PathVariable Long id) {
        return Result.ok(articleService.detail(id, false));
    }

    @PostMapping
    public Result<Article> create(@AuthenticationPrincipal BlogPrincipal principal, @RequestBody ArticleRequest request) {
        return Result.ok(articleService.saveArticle(null, principal.userId(), request));
    }

    @PutMapping("/{id}")
    public Result<Article> update(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id,
                                  @RequestBody ArticleRequest request) {
        return Result.ok(articleService.saveArticle(id, principal.userId(), request));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> remove(@RequestBody List<Long> ids) {
        articleService.removeBatch(ids);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> removeMine(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id) {
        articleService.removeByOwner(id, principal.userId());
        return Result.ok();
    }
}
