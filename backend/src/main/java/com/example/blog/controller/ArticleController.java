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
    public Result<ArticleDetailResponse> detail(@PathVariable Long id) {
        return Result.ok(articleService.detailResponse(id, false));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Article> createByAdmin(@AuthenticationPrincipal BlogPrincipal principal, @RequestBody ArticleRequest request) {
        Article article = articleService.saveArticleByAdmin(null, principal.userId(), request);
        operationLogService.record(principal, "CREATE", "ARTICLE", article.getId(), article.getTitle());
        return Result.ok(article);
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Article> updateByAdmin(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id,
                                         @RequestBody ArticleRequest request) {
        Article article = articleService.saveArticleByAdmin(id, principal.userId(), request);
        operationLogService.record(principal, "UPDATE", "ARTICLE", id, article.getTitle());
        return Result.ok(article);
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
                                        @RequestParam String status,
                                        @RequestParam(required = false) String reason) {
        Article article = articleService.updateStatusByAdmin(id, status, reason);
        String detail = status + " - " + article.getTitle();
        if (reason != null && !reason.isBlank()) {
            detail += " - " + reason.trim();
        }
        operationLogService.record(principal, "AUDIT", "ARTICLE", id, detail);
        return Result.ok(article);
    }

    @PutMapping("/{id}/recommendation")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Article> updateRecommendation(@AuthenticationPrincipal BlogPrincipal principal,
                                                @PathVariable Long id,
                                                @RequestParam Boolean recommended,
                                                @RequestParam(defaultValue = "0") Integer recommendSort) {
        Article article = articleService.updateRecommendationByAdmin(id, recommended, recommendSort);
        String detail = (Boolean.TRUE.equals(recommended) ? "推荐到首页" : "取消首页推荐") + " - " + article.getTitle();
        operationLogService.record(principal, "RECOMMEND", "ARTICLE", id, detail);
        return Result.ok(article);
    }

    @DeleteMapping("/{id}")
    public Result<Void> removeMine(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id) {
        articleService.removeByOwner(id, principal.userId());
        return Result.ok();
    }
}
