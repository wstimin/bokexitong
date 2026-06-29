package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.common.Result;
import com.example.blog.dto.AdminUserRequest;
import com.example.blog.dto.DashboardStats;
import com.example.blog.dto.PasswordResetRequest;
import com.example.blog.entity.*;
import com.example.blog.mapper.*;
import com.example.blog.service.DashboardService;
import com.example.blog.service.InteractionService;
import com.example.blog.service.ArticleService;
import com.example.blog.service.AuthService;
import com.example.blog.security.BlogPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin")
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

    public AdminController(DashboardService dashboardService, CategoryMapper categoryMapper, TagMapper tagMapper,
                           ImageResourceMapper imageResourceMapper, BlogUserMapper userMapper,
                           ArticleMapper articleMapper, CommentMapper commentMapper, LikeRecordMapper likeRecordMapper,
                           FavoriteMapper favoriteMapper, InteractionService interactionService, ArticleService articleService,
                           AuthService authService) {
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
    }

    @GetMapping("/dashboard")
    public Result<DashboardStats> dashboard() {
        return Result.ok(dashboardService.stats());
    }

    @GetMapping("/categories")
    public Result<Page<Category>> categories(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(categoryMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort)));
    }

    @PostMapping("/categories")
    public Result<Category> saveCategory(@RequestBody Category category) {
        if (category.getId() == null) {
            category.setCreatedAt(LocalDateTime.now());
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return Result.ok(category);
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/tags")
    public Result<Page<Tag>> tags(@RequestParam(defaultValue = "1") long current,
                                  @RequestParam(defaultValue = "10") long size) {
        return Result.ok(tagMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Tag>().orderByDesc(Tag::getCreatedAt)));
    }

    @PostMapping("/tags")
    public Result<Tag> saveTag(@RequestBody Tag tag) {
        if (tag.getId() == null) {
            tag.setCreatedAt(LocalDateTime.now());
            tagMapper.insert(tag);
        } else {
            tagMapper.updateById(tag);
        }
        return Result.ok(tag);
    }

    @DeleteMapping("/tags/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        tagMapper.deleteById(id);
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
    public Result<ImageResource> saveImage(@RequestBody ImageResource image) {
        LocalDateTime now = LocalDateTime.now();
        image.setUpdatedAt(now);
        image.setEnabled(image.getEnabled() == null ? 1 : image.getEnabled());
        if (image.getId() == null) {
            image.setCreatedAt(now);
            imageResourceMapper.insert(image);
        } else {
            imageResourceMapper.updateById(image);
        }
        return Result.ok(image);
    }

    @DeleteMapping("/images/{id}")
    public Result<Void> deleteImage(@PathVariable Long id) {
        imageResourceMapper.deleteById(id);
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
        return Result.ok();
    }

    @PutMapping("/users/{id}")
    public Result<BlogUser> updateUser(@AuthenticationPrincipal BlogPrincipal principal, @PathVariable Long id,
                                       @RequestBody AdminUserRequest request) {
        protectAdminAccess(principal == null ? null : principal.userId(), id, request.getRole(), request.getStatus());
        return Result.ok(authService.updateUserByAdmin(id, request));
    }

    @PutMapping("/users/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody PasswordResetRequest request) {
        authService.resetPasswordByAdmin(id, request);
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
    public Result<Void> auditComment(@PathVariable Long id, @RequestParam String status) {
        interactionService.auditComment(id, status);
        return Result.ok();
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        interactionService.deleteComment(id);
        return Result.ok();
    }
}
