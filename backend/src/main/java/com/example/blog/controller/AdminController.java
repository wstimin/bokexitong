package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.common.Result;
import com.example.blog.dto.DashboardStats;
import com.example.blog.entity.*;
import com.example.blog.mapper.*;
import com.example.blog.service.DashboardService;
import com.example.blog.service.InteractionService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final DashboardService dashboardService;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ImageResourceMapper imageResourceMapper;
    private final BlogUserMapper userMapper;
    private final InteractionService interactionService;

    public AdminController(DashboardService dashboardService, CategoryMapper categoryMapper, TagMapper tagMapper,
                           ImageResourceMapper imageResourceMapper, BlogUserMapper userMapper,
                           InteractionService interactionService) {
        this.dashboardService = dashboardService;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.imageResourceMapper = imageResourceMapper;
        this.userMapper = userMapper;
        this.interactionService = interactionService;
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
                                        @RequestParam(defaultValue = "10") long size) {
        Page<BlogUser> page = userMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<BlogUser>().orderByDesc(BlogUser::getCreatedAt));
        page.getRecords().forEach(user -> user.setPassword(null));
        return Result.ok(page);
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> userStatus(@PathVariable Long id, @RequestParam Integer status) {
        BlogUser user = userMapper.selectById(id);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.ok();
    }

    @GetMapping("/comments")
    public Result<Page<Comment>> comments(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.ok(interactionService.comments(null, current, size));
    }

    @PutMapping("/comments/{id}/status")
    public Result<Void> auditComment(@PathVariable Long id, @RequestParam String status) {
        interactionService.auditComment(id, status);
        return Result.ok();
    }
}
