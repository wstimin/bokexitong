package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.common.Result;
import com.example.blog.dto.InstallRequest;
import com.example.blog.dto.InstallStatusResponse;
import com.example.blog.entity.SiteSetting;
import com.example.blog.mapper.SiteSettingMapper;
import com.example.blog.service.InstallService;
import com.example.blog.service.SiteSettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/install")
public class InstallController {
    private final SiteSettingService siteSettingService;
    private final SiteSettingMapper siteSettingMapper;
    private final InstallService installService;

    public InstallController(SiteSettingService siteSettingService, SiteSettingMapper siteSettingMapper,
                             InstallService installService) {
        this.siteSettingService = siteSettingService;
        this.siteSettingMapper = siteSettingMapper;
        this.installService = installService;
    }

    @GetMapping("/status")
    public Result<InstallStatusResponse> status() {
        if (installService.hasRuntimeConfig()) {
            return Result.ok(new InstallStatusResponse(true, currentSiteName(), currentDomain(), true, true));
        }
        if (installService.isRestartAfterInstallEnabled()) {
            // A supervised web package without a runtime config is still waiting
            // for its first installation. Avoid querying the placeholder startup
            // datasource because it may not be reachable from the panel runtime.
            return Result.ok(new InstallStatusResponse(false, "博客系统", "", false, false));
        }
        return Result.ok(new InstallStatusResponse(siteSettingService.isInstalled(), currentSiteName(), currentDomain(), databaseReady(), false));
    }

    @PostMapping
    public Result<InstallStatusResponse> install(@RequestBody InstallRequest request) {
        // A supervised web installation must use the submitted database details.
        // Other deployment modes retain their existing datasource-based check.
        if (installService.hasRuntimeConfig()
                || (!installService.isRestartAfterInstallEnabled() && siteSettingService.isInstalled())) {
            return Result.ok(new InstallStatusResponse(true, currentSiteName(), currentDomain(), true, installService.hasRuntimeConfig()));
        }
        installService.install(request);
        installService.restartSoon();
        return Result.ok(new InstallStatusResponse(true, fallback(request.getSiteName(), "博客系统"), fallback(request.getDomain(), ""), true, true));
    }

    private boolean databaseReady() {
        try {
            siteSettingMapper.selectCount(null);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String currentSiteName() {
        return valueOf("siteName", "博客系统");
    }

    private String currentDomain() {
        return valueOf("blogDomain", "");
    }

    private String valueOf(String key, String fallback) {
        try {
            SiteSetting row = siteSettingMapper.selectOne(new LambdaQueryWrapper<SiteSetting>()
                    .eq(SiteSetting::getSettingKey, key)
                    .last("LIMIT 1"));
            if (row == null || isBlank(row.getSettingValue())) {
                return fallback;
            }
            return row.getSettingValue();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String fallback(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
