package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.common.Result;
import com.example.blog.dto.AdminUserRequest;
import com.example.blog.dto.DashboardStats;
import com.example.blog.dto.PasswordResetRequest;
import com.example.blog.entity.Article;
import com.example.blog.entity.BlogUser;
import com.example.blog.entity.Category;
import com.example.blog.entity.Comment;
import com.example.blog.entity.Favorite;
import com.example.blog.entity.ImageResource;
import com.example.blog.entity.LikeRecord;
import com.example.blog.entity.Tag;
import com.example.blog.mapper.ArticleMapper;
import com.example.blog.mapper.BlogUserMapper;
import com.example.blog.mapper.CategoryMapper;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.mapper.FavoriteMapper;
import com.example.blog.mapper.ImageResourceMapper;
import com.example.blog.mapper.LikeRecordMapper;
import com.example.blog.mapper.TagMapper;
import com.example.blog.security.BlogPrincipal;
import com.example.blog.service.ArticleService;
import com.example.blog.service.AuthService;
import com.example.blog.service.DashboardService;
import com.example.blog.service.EmailCodeService;
import com.example.blog.service.InteractionService;
import com.example.blog.service.OperationLogService;
import com.example.blog.service.SiteSettingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final DashboardService dashboardService;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ImageResourceMapper imageResourceMapper;
    private final BlogUserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteMapper favoriteMapper;
    private final InteractionService interactionService;
    private final ArticleService articleService;
    private final AuthService authService;
    private final SiteSettingService siteSettingService;
    private final OperationLogService operationLogService;
    private final EmailCodeService emailCodeService;

    public AdminController(DashboardService dashboardService, CategoryMapper categoryMapper, TagMapper tagMapper,
                           ImageResourceMapper imageResourceMapper, BlogUserMapper userMapper,
                           ArticleMapper articleMapper, CommentMapper commentMapper, LikeRecordMapper likeRecordMapper,
                           FavoriteMapper favoriteMapper, InteractionService interactionService, ArticleService articleService,
                           AuthService authService, SiteSettingService siteSettingService, OperationLogService operationLogService,
                           EmailCodeService emailCodeService) {
        this.dashboardService = dashboardService;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.imageResourceMapper = imageResourceMapper;
        this.userMapper = userMapper;
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteMapper = favoriteMapper;
        this.interactionService = interactionService;
        this.articleService = articleService;
        this.authService = authService;
        this.siteSettingService = siteSettingService;
        this.operationLogService = operationLogService;
        this.emailCodeService = emailCodeService;
    }

    @GetMapping("/dashboard")
    public Result<DashboardStats> dashboard() {
        return Result.ok(dashboardService.stats());
    }

    @GetMapping("/settings")
    public Result<Map<String, String>> settings() {
        return Result.ok(siteSettingService.adminSettings());
    }

    @GetMapping("/site-settings")
    public Result<Map<String, String>> siteSettings() {
        return Result.ok(siteSettingService.adminSiteSettings());
    }

    @GetMapping("/mail-settings")
    public Result<Map<String, String>> mailSettings() {
        return Result.ok(siteSettingService.adminMailSettings());
    }

    @PutMapping("/settings")
    public Result<Map<String, String>> saveSettings(@AuthenticationPrincipal BlogPrincipal principal,
                                                    @RequestBody Map<String, String> payload) {
        Map<String, String> settings = siteSettingService.save(payload);
        operationLogService.record(principal, "UPDATE", "SITE_SETTING", null, "更新站点设置");
        return Result.ok(settings);
    }

    @PutMapping("/site-settings")
    public Result<Map<String, String>> saveSiteSettings(@AuthenticationPrincipal BlogPrincipal principal,
                                                        @RequestBody Map<String, String> payload) {
        Map<String, String> settings = siteSettingService.saveSite(payload);
        operationLogService.record(principal, "UPDATE", "SITE_SETTING", null, "更新站点设置");
        return Result.ok(settings);
    }

    @PutMapping("/mail-settings")
    public Result<Map<String, String>> saveMailSettings(@AuthenticationPrincipal BlogPrincipal principal,
                                                        @RequestBody Map<String, String> payload) {
        Map<String, String> settings = siteSettingService.saveMail(payload);
        operationLogService.record(principal, "UPDATE", "MAIL_SETTING", null, "更新邮箱设置");
        return Result.ok(settings);
    }

    @PostMapping("/settings/test-mail")
    public Result<Void> testMail(@AuthenticationPrincipal BlogPrincipal principal,
                                 @RequestBody Map<String, String> payload) {
        emailCodeService.sendTestMail(payload.get("email"));
        operationLogService.record(principal, "TEST", "MAIL_SETTING", null, payload.get("email"));
        return Result.ok();
    }

    @PostMapping("/mail-settings/test-mail")
    public Result<Void> testMailFromMailSettings(@AuthenticationPrincipal BlogPrincipal principal,
                                                 @RequestBody Map<String, String> payload) {
        emailCodeService.sendTestMail(payload.get("email"));
        operationLogService.record(principal, "TEST", "MAIL_SETTING", null, payload.get("email"));
        return Result.ok();
    }

    @GetMapping("/operation-logs")
    public Result<Page<com.example.blog.entity.OperationLog>> operationLogs(@RequestParam(defaultValue = "1") long current,
                                                                           @RequestParam(defaultValue = "10") long size,
                                                                           @RequestParam(required = false) String action,
                                                                           @RequestParam(required = false) String targetType,
                                                                           @RequestParam(required = false) String keyword) {
        return Result.ok(operationLogService.page(current, size, action, targetType, keyword));
    }

    @GetMapping("/categories")
    public Result<Page<Category>> categories(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(categoryMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)));
    }

    @PostMapping("/categories")
    public Result<Category> saveCategory(@AuthenticationPrincipal BlogPrincipal principal, @RequestBody Category category) {
        boolean creating = category.getId() == null;
        if (category.getId() == null) {
            category.setCreatedAt(LocalDateTime.now());
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        operationLogService.record(principal, creating ? "CREATE" : "UPDATE", "CATEGORY", category.getId(), category.getName());
        return Result.ok(category);
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id) {
        categoryMapper.deleteById(id);
        operationLogService.record(principal, "DELETE", "CATEGORY", id, "删除分类");
        return Result.ok();
    }

    @GetMapping("/tags")
    public Result<Page<Tag>> tags(@RequestParam(defaultValue = "1") long current,
                                  @RequestParam(defaultValue = "10") long size) {
        return Result.ok(tagMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Tag>()
                .orderByDesc(Tag::getCreatedAt)));
    }

    @PostMapping("/tags")
    public Result<Tag> saveTag(@AuthenticationPrincipal BlogPrincipal principal, @RequestBody Tag tag) {
        boolean creating = tag.getId() == null;
        if (tag.getId() == null) {
            tag.setCreatedAt(LocalDateTime.now());
            tagMapper.insert(tag);
        } else {
            tagMapper.updateById(tag);
        }
        operationLogService.record(principal, creating ? "CREATE" : "UPDATE", "TAG", tag.getId(), tag.getName());
        return Result.ok(tag);
    }

    @DeleteMapping("/tags/{id}")
    public Result<Void> deleteTag(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id) {
        tagMapper.deleteById(id);
        operationLogService.record(principal, "DELETE", "TAG", id, "删除标签");
        return Result.ok();
    }

    @GetMapping("/images")
    public Result<Page<ImageResource>> images(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String type) {
        return Result.ok(imageResourceMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<ImageResource>()
                .eq(type != null && !type.isBlank(), ImageResource::getType, type)
                .orderByAsc(ImageResource::getSort)
                .orderByDesc(ImageResource::getCreatedAt)));
    }

    @PostMapping("/images")
    public Result<ImageResource> saveImage(@AuthenticationPrincipal BlogPrincipal principal, @RequestBody ImageResource image) {
        boolean creating = image.getId() == null;
        LocalDateTime now = LocalDateTime.now();
        image.setUpdatedAt(now);
        image.setEnabled(image.getEnabled() == null ? 1 : image.getEnabled());
        if (image.getId() == null) {
            image.setCreatedAt(now);
            imageResourceMapper.insert(image);
        } else {
            imageResourceMapper.updateById(image);
        }
        operationLogService.record(principal, creating ? "CREATE" : "UPDATE", "IMAGE", image.getId(), image.getTitle());
        return Result.ok(image);
    }

    @DeleteMapping("/images/{id}")
    public Result<Void> deleteImage(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id) {
        imageResourceMapper.deleteById(id);
        operationLogService.record(principal, "DELETE", "IMAGE", id, "删除图片资源");
        return Result.ok();
    }

    @GetMapping("/users")
    public Result<Page<BlogUser>> users(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String role,
                                        @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<BlogUser> wrapper = new LambdaQueryWrapper<BlogUser>()
                .and(keyword != null && !keyword.isBlank(), w -> w.like(BlogUser::getUsername, keyword)
                        .or().like(BlogUser::getNickname, keyword)
                        .or().like(BlogUser::getEmail, keyword))
                .eq(role != null && !role.isBlank(), BlogUser::getRole, role)
                .eq(status != null, BlogUser::getStatus, status)
                .orderByDesc(BlogUser::getCreatedAt);
        Page<BlogUser> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        page.getRecords().forEach(user -> user.setPassword(null));
        return Result.ok(page);
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> userStatus(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id, @RequestParam Integer status) {
        protectAdminAccess(principal == null ? null : principal.userId(), id, null, status);
        BlogUser user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        operationLogService.record(principal, status == 1 ? "ENABLE" : "DISABLE", "USER", id, user.getUsername());
        return Result.ok();
    }

    @PutMapping("/users/{id}")
    public Result<BlogUser> updateUser(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id,
                                       @RequestBody AdminUserRequest request) {
        protectAdminAccess(principal == null ? null : principal.userId(), id, request.getRole(), request.getStatus());
        BlogUser user = authService.updateUserByAdmin(id, request);
        operationLogService.record(principal, "UPDATE", "USER", id, user.getUsername());
        return Result.ok(user);
    }

    @PostMapping("/users")
    public Result<BlogUser> createUser(@AuthenticationPrincipal BlogPrincipal principal,
                                       @RequestBody AdminUserRequest request) {
        BlogUser user = authService.createUserByAdmin(request);
        operationLogService.record(principal, "CREATE", "USER", user.getId(), user.getUsername());
        return Result.ok(user);
    }

    @PutMapping("/users/{id}/password")
    public Result<Void> resetPassword(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id, @RequestBody PasswordResetRequest request) {
        authService.resetPasswordByAdmin(id, request);
        operationLogService.record(principal, "RESET_PASSWORD", "USER", id, "重置用户密码");
        return Result.ok();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id) {
        if (principal != null && principal.userId().equals(id)) {
            throw new IllegalArgumentException("不能删除当前登录的管理员账号");
        }
        BlogUser user = userMapper.selectById(id);
        if (user == null) {
            return Result.ok();
        }
        if ("ADMIN".equals(user.getRole())) {
            Long adminCount = userMapper.selectCount(new LambdaQueryWrapper<BlogUser>()
                    .eq(BlogUser::getRole, "ADMIN"));
            if (adminCount <= 1) {
                throw new IllegalArgumentException("至少需要保留一个管理员账号");
            }
        }
        List<Long> articleIds = articleMapper.selectList(new LambdaQueryWrapper<Article>().eq(Article::getUserId, id))
                .stream().map(Article::getId).toList();
        articleService.removeBatch(articleIds);
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, id));
        likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>().eq(LikeRecord::getUserId, id));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, id));
        userMapper.deleteById(id);
        operationLogService.record(principal, "DELETE", "USER", id, user.getUsername());
        return Result.ok();
    }

    private void protectAdminAccess(Long currentUserId, Long targetUserId, String nextRole, Integer nextStatus) {
        BlogUser target = userMapper.selectById(targetUserId);
        if (target == null || !"ADMIN".equals(target.getRole())) {
            return;
        }
        boolean selfLock = currentUserId != null && currentUserId.equals(targetUserId)
                && ((nextRole != null && !"ADMIN".equals(nextRole)) || (nextStatus != null && nextStatus == 0));
        if (selfLock) {
            throw new IllegalArgumentException("不能降级或封禁当前登录的管理员账号");
        }
        boolean removingAdmin = (nextRole != null && !"ADMIN".equals(nextRole)) || (nextStatus != null && nextStatus == 0);
        boolean affectsActiveAdmin = target.getStatus() != null && target.getStatus() == 1 && removingAdmin;
        if (affectsActiveAdmin) {
            Long activeAdminCount = userMapper.selectCount(new LambdaQueryWrapper<BlogUser>()
                    .eq(BlogUser::getRole, "ADMIN")
                    .eq(BlogUser::getStatus, 1));
            if (activeAdminCount <= 1) {
                throw new IllegalArgumentException("至少需要保留一个可用的管理员账号");
            }
        }
    }

    @GetMapping("/comments")
    public Result<Page<Comment>> comments(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String status) {
        return Result.ok(interactionService.comments(null, current, size, status));
    }

    @PutMapping("/comments/{id}/status")
    public Result<Void> auditComment(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id,
                                     @RequestParam String status,
                                     @RequestParam(required = false) String reason) {
        interactionService.auditComment(id, status, reason);
        String detail = reason == null || reason.isBlank() ? status : status + " - " + reason.trim();
        operationLogService.record(principal, "AUDIT", "COMMENT", id, detail);
        return Result.ok();
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id) {
        interactionService.deleteComment(id);
        operationLogService.record(principal, "DELETE", "COMMENT", id, "删除评论");
        return Result.ok();
    }
}
