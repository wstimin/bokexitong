package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.common.Result;
import com.example.blog.entity.*;
import com.example.blog.mapper.CategoryMapper;
import com.example.blog.mapper.ImageResourceMapper;
import com.example.blog.mapper.TagMapper;
import com.example.blog.service.ArticleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/portal")
public class PortalController {
    private final ArticleService articleService;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ImageResourceMapper imageResourceMapper;

    public PortalController(ArticleService articleService, CategoryMapper categoryMapper, TagMapper tagMapper,
                            ImageResourceMapper imageResourceMapper) {
        this.articleService = articleService;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.imageResourceMapper = imageResourceMapper;
    }

    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        Map<String, Object> data = new HashMap<>();
        data.put("articles", articleService.page(1, 6, null, "PUBLISHED", null).getRecords());
        data.put("categories", categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort)));
        data.put("tags", tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByDesc(Tag::getCreatedAt)));
        data.put("hero", imageResourceMapper.selectList(new LambdaQueryWrapper<ImageResource>()
                .eq(ImageResource::getEnabled, 1)
                .eq(ImageResource::getType, "HERO")
                .orderByAsc(ImageResource::getSort)));
        return Result.ok(data);
    }

    @GetMapping("/articles")
    public Result<Page<Article>> articles(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long categoryId) {
        return Result.ok(articleService.page(current, size, keyword, "PUBLISHED", categoryId));
    }

    @GetMapping("/articles/{id}")
    public Result<Article> detail(@PathVariable Long id) {
        return Result.ok(articleService.detail(id, true));
    }
}
