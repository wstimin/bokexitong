package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.common.Result;
import com.example.blog.dto.InstallRequest;
import com.example.blog.dto.InstallStatusResponse;
import com.example.blog.entity.BlogUser;
import com.example.blog.entity.SiteSetting;
import com.example.blog.mapper.BlogUserMapper;
import com.example.blog.mapper.SiteSettingMapper;
import com.example.blog.service.SiteSettingService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/install")
public class InstallController {
    private final SiteSettingService siteSettingService;
    private final SiteSettingMapper siteSettingMapper;
    private final BlogUserMapper blogUserMapper;
    private final PasswordEncoder passwordEncoder;

    public InstallController(SiteSettingService siteSettingService, SiteSettingMapper siteSettingMapper,
                             BlogUserMapper blogUserMapper, PasswordEncoder passwordEncoder) {
        this.siteSettingService = siteSettingService;
        this.siteSettingMapper = siteSettingMapper;
        this.blogUserMapper = blogUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/status")
    public Result<InstallStatusResponse> status() {
        return Result.ok(new InstallStatusResponse(siteSettingService.isInstalled(), currentSiteName(), currentDomain()));
    }

    @PostMapping
    public Result<InstallStatusResponse> install(@RequestBody InstallRequest request) {
        if (siteSettingService.isInstalled()) {
            return Result.ok(new InstallStatusResponse(true, currentSiteName(), currentDomain()));
        }
        if (request == null || isBlank(request.getAdminPassword())) {
            throw new IllegalArgumentException("请完整填写安装信息");
        }
        if (request.getAdminPassword().trim().length() < 8) {
            throw new IllegalArgumentException("管理员密码至少 8 位");
        }
        BlogUser admin = blogUserMapper.selectOne(new LambdaQueryWrapper<BlogUser>()
                .eq(BlogUser::getUsername, "admin")
                .last("LIMIT 1"));
        if (admin == null) {
            admin = new BlogUser();
            admin.setUsername("admin");
            admin.setNickname("站长");
            admin.setPassword(passwordEncoder.encode(request.getAdminPassword().trim()));
            admin.setRole("ADMIN");
            admin.setStatus(1);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            blogUserMapper.insert(admin);
        } else {
            admin.setPassword(passwordEncoder.encode(request.getAdminPassword().trim()));
            admin.setUpdatedAt(LocalDateTime.now());
            blogUserMapper.updateById(admin);
        }
        upsert("siteName", fallback(request.getSiteName(), "博客系统"));
        upsert("adminLoginPath", "/admin/login");
        if (!isBlank(request.getDomain())) {
            upsert("blogDomain", request.getDomain().trim());
        }
        upsert("installationComplete", "true");
        siteSettingService.markInstalled();
        return Result.ok(new InstallStatusResponse(true, currentSiteName(), currentDomain()));
    }

    private void upsert(String key, String value) {
        SiteSetting row = siteSettingMapper.selectOne(new LambdaQueryWrapper<SiteSetting>()
                .eq(SiteSetting::getSettingKey, key)
                .last("LIMIT 1"));
        if (row == null) {
            row = new SiteSetting();
            row.setSettingKey(key);
            row.setSettingValue(value);
            row.setDescription(key);
            row.setUpdatedAt(LocalDateTime.now());
            siteSettingMapper.insert(row);
        } else {
            row.setSettingValue(value);
            row.setUpdatedAt(LocalDateTime.now());
            siteSettingMapper.updateById(row);
        }
    }

    private String currentSiteName() {
        return valueOf("siteName", "博客系统");
    }

    private String currentDomain() {
        return valueOf("blogDomain", "");
    }

    private String valueOf(String key, String fallback) {
        SiteSetting row = siteSettingMapper.selectOne(new LambdaQueryWrapper<SiteSetting>()
                .eq(SiteSetting::getSettingKey, key)
                .last("LIMIT 1"));
        if (row == null || isBlank(row.getSettingValue())) {
            return fallback;
        }
        return row.getSettingValue();
    }

    private String fallback(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
