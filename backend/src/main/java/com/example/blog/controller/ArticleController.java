package com.example.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.common.Result;
import com.example.blog.dto.ArticleDetailResponse;
import com.example.blog.dto.ArticleRequest;
import com.example.blog.entity.Article;
import com.example.blog.security.BlogPrincipal;
import com.example.blog.service.ArticleService;
import com.example.blog.service.OperationLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/articles")
public class ArticleController {
    private final ArticleService articleService;
    private final OperationLogService operationLogService;

    public ArticleController(ArticleService articleService, OperationLogService operationLogService) {
        this.articleService = articleService;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<Article>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) Long categoryId) {
        return Result.ok(articleService.page(current, size, keyword, status, categoryId, null));
    }

    @GetMapping("/mine")
    public Result<Page<ArticleDetailResponse>> mine(@AuthenticationPrincipal BlogPrincipal principal,
                                                    @RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String status) {
        return Result.ok(articleService.userDetailPage(principal.userId(), current, size, keyword, status));
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
    public Result<Void> remove(@AuthenticationPrincipal BlogPrincipal principal, @RequestBody List<Long> ids) {
        articleService.removeBatch(ids);
        operationLogService.record(principal, "DELETE", "ARTICLE", null, "批量删除文章：" + ids);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Article> updateStatus(@AuthenticationPrincipal BlogPrincipal principal,
                                        @PathVariable Long id,
                                        @RequestParam String status) {
        Article article = articleService.updateStatusByAdmin(id, status);
        operationLogService.record(principal, "AUDIT", "ARTICLE", id, status + " - " + article.getTitle());
        return Result.ok(article);
    }

    @DeleteMapping("/{id}")
    public Result<Void> removeMine(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id) {
        articleService.removeByOwner(id, principal.userId());
        return Result.ok();
    }
}
