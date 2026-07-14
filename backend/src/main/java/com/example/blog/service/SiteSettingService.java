package com.example.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.entity.SiteSetting;
import com.example.blog.mapper.SiteSettingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SiteSettingService {
    public static final String SITE_NAME = "siteName";
    public static final String HERO_TITLE = "heroTitle";
    public static final String HERO_SUBTITLE = "heroSubtitle";
    public static final String HERO_BADGE = "heroBadge";
    public static final String BACKGROUND_URL = "backgroundUrl";
    public static final String ALLOW_REGISTER = "allowRegister";

    private final SiteSettingMapper siteSettingMapper;

    public SiteSettingService(SiteSettingMapper siteSettingMapper) {
        this.siteSettingMapper = siteSettingMapper;
    }

    public Map<String, String> publicSettings() {
        Map<String, String> settings = defaults();
        List<SiteSetting> rows;
        try {
            rows = siteSettingMapper.selectList(null);
        } catch (Exception ignored) {
            return settings;
        }
        for (SiteSetting row : rows) {
            if (settings.containsKey(row.getSettingKey()) && row.getSettingValue() != null) {
                settings.put(row.getSettingKey(), row.getSettingValue());
            }
        }
        return settings;
    }

    public Map<String, String> save(Map<String, String> payload) {
        Map<String, String> next = defaults();
        for (String key : next.keySet()) {
            if (payload.containsKey(key)) {
                next.put(key, ALLOW_REGISTER.equals(key) ? normalizeBoolean(payload.get(key), next.get(key)) : clean(payload.get(key), next.get(key)));
            }
            upsert(key, next.get(key));
        }
        return publicSettings();
    }

    public boolean allowRegister() {
        return "true".equalsIgnoreCase(publicSettings().get(ALLOW_REGISTER));
    }

    private Map<String, String> defaults() {
        Map<String, String> settings = new HashMap<>();
        settings.put(SITE_NAME, "博客系统");
        settings.put(HERO_TITLE, "博客系统");
        settings.put(HERO_SUBTITLE, "用卡片浏览公开文章，点开后再阅读完整内容。登录后可以进入用户中心创作、管理自己的文章。");
        settings.put(HERO_BADGE, "Personal Blog");
        settings.put(BACKGROUND_URL, "");
        settings.put(ALLOW_REGISTER, "true");
        return settings;
    }

    private String clean(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String normalizeBoolean(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return trimmed.toLowerCase();
        }
        return fallback;
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
}
