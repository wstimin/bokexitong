package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.common.Result;
import com.example.blog.dto.ArticleCardResponse;
import com.example.blog.dto.ArticleDetailResponse;
import com.example.blog.entity.*;
import com.example.blog.mapper.CategoryMapper;
import com.example.blog.mapper.ImageResourceMapper;
import com.example.blog.mapper.TagMapper;
import com.example.blog.service.ArticleService;
import com.example.blog.service.SiteSettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portal")
public class PortalController {
    private final ArticleService articleService;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ImageResourceMapper imageResourceMapper;
    private final SiteSettingService siteSettingService;

    public PortalController(ArticleService articleService, CategoryMapper categoryMapper, TagMapper tagMapper,
                            ImageResourceMapper imageResourceMapper, SiteSettingService siteSettingService) {
        this.articleService = articleService;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.imageResourceMapper = imageResourceMapper;
        this.siteSettingService = siteSettingService;
    }

    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        Map<String, Object> data = new HashMap<>();
        Map<String, String> settings = siteSettingService.publicSettings();
        data.put("articles", articleService.publicCardPage(1, 6, null, null, null).getRecords());
        data.put("recommendedArticles", articleService.recommendedCards(6));
        data.put("categories", categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort)));
        data.put("tags", tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByDesc(Tag::getCreatedAt)));
        data.put("settings", settings);
        data.put("hero", imageResourceMapper.selectList(new LambdaQueryWrapper<ImageResource>()
                .eq(ImageResource::getEnabled, 1)
                .eq(ImageResource::getType, "HERO")
                .orderByAsc(ImageResource::getSort)));
        String logoUrl = settings.get(SiteSettingService.LOGO_URL);
        if (logoUrl != null && !logoUrl.isBlank()) {
            ImageResource logo = new ImageResource();
            logo.setTitle("站点 Logo");
            logo.setUrl(logoUrl);
            logo.setType("LOGO");
            logo.setEnabled(1);
            data.put("logo", logo);
        } else {
            List<ImageResource> logos = imageResourceMapper.selectList(new LambdaQueryWrapper<ImageResource>()
                    .eq(ImageResource::getEnabled, 1)
                    .eq(ImageResource::getType, "LOGO")
                    .orderByAsc(ImageResource::getSort)
                    .orderByDesc(ImageResource::getCreatedAt));
            data.put("logo", logos.isEmpty() ? null : logos.get(0));
        }
        return Result.ok(data);
    }

    @GetMapping("/articles")
    public Result<Page<ArticleCardResponse>> articles(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Long categoryId,
                                                      @RequestParam(required = false) Long tagId) {
        return Result.ok(articleService.publicCardPage(current, size, keyword, categoryId, tagId));
    }

    @GetMapping("/articles/{id}")
    public Result<ArticleDetailResponse> detail(@PathVariable Long id) {
        return Result.ok(articleService.publicDetailResponse(id));
    }
}
