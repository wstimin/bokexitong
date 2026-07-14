package com.example.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.entity.SiteSetting;
import com.example.blog.mapper.SiteSettingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SiteSettingService {
    public static final String SITE_NAME = "siteName";
    public static final String HERO_TITLE = "heroTitle";
    public static final String HERO_SUBTITLE = "heroSubtitle";
    public static final String HERO_BADGE = "heroBadge";
    public static final String BACKGROUND_URL = "backgroundUrl";
    public static final String LOGO_URL = "logoUrl";
    public static final String SEO_DESCRIPTION = "seoDescription";
    public static final String SEO_KEYWORDS = "seoKeywords";
    public static final String ICP_BEIAN = "icpBeian";
    public static final String FOOTER_TEXT = "footerText";
    public static final String ALLOW_REGISTER = "allowRegister";
    public static final String MAIL_ENABLED = "mailEnabled";
    public static final String MAIL_HOST = "mailHost";
    public static final String MAIL_PORT = "mailPort";
    public static final String MAIL_USERNAME = "mailUsername";
    public static final String MAIL_PASSWORD = "mailPassword";
    public static final String MAIL_FROM_NAME = "mailFromName";
    public static final String MAIL_SMTP_AUTH = "mailSmtpAuth";
    public static final String MAIL_STARTTLS_ENABLE = "mailStarttlsEnable";
    public static final String MAIL_SSL_ENABLE = "mailSslEnable";

    private static final Set<String> PUBLIC_KEYS = Set.of(
            SITE_NAME, HERO_TITLE, HERO_SUBTITLE, HERO_BADGE, BACKGROUND_URL, LOGO_URL,
            SEO_DESCRIPTION, SEO_KEYWORDS, ICP_BEIAN, FOOTER_TEXT, ALLOW_REGISTER
    );

    private final SiteSettingMapper siteSettingMapper;

    public SiteSettingService(SiteSettingMapper siteSettingMapper) {
        this.siteSettingMapper = siteSettingMapper;
    }

    public Map<String, String> publicSettings() {
        return loadSettings(PUBLIC_KEYS, false);
    }

    public Map<String, String> adminSettings() {
        return loadSettings(defaults().keySet(), true);
    }

    public Map<String, String> mailSettings() {
        return loadSettings(defaults().keySet(), false);
    }

    private Map<String, String> loadSettings(Set<String> keys, boolean hideSecret) {
        Map<String, String> source = defaults();
        Map<String, String> settings = new HashMap<>();
        for (String key : keys) {
            settings.put(key, source.getOrDefault(key, ""));
        }
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
        if (hideSecret && settings.containsKey(MAIL_PASSWORD)) {
            settings.put(MAIL_PASSWORD, "");
        }
        return settings;
    }

    public Map<String, String> save(Map<String, String> payload) {
        Map<String, String> next = defaults();
        Map<String, String> current = mailSettings();
        for (String key : next.keySet()) {
            if (payload.containsKey(key)) {
                if (MAIL_PASSWORD.equals(key) && clean(payload.get(key), "").isEmpty()) {
                    next.put(key, current.getOrDefault(key, ""));
                } else if (ALLOW_REGISTER.equals(key) || MAIL_ENABLED.equals(key) || MAIL_SMTP_AUTH.equals(key)
                        || MAIL_STARTTLS_ENABLE.equals(key) || MAIL_SSL_ENABLE.equals(key)) {
                    next.put(key, normalizeBoolean(payload.get(key), next.get(key)));
                } else {
                    next.put(key, clean(payload.get(key), next.get(key)));
                }
            } else if (MAIL_PASSWORD.equals(key)) {
                next.put(key, current.getOrDefault(key, ""));
            }
            upsert(key, next.get(key));
        }
        return adminSettings();
    }

    public boolean allowRegister() {
        return "true".equalsIgnoreCase(publicSettings().get(ALLOW_REGISTER));
    }

    private Map<String, String> defaults() {
        Map<String, String> settings = new HashMap<>();
        settings.put(SITE_NAME, "博客系统");
        settings.put(HERO_TITLE, "博客系统");
        settings.put(HERO_SUBTITLE, "用卡片浏览公开文章，点开后再阅读完整内容。登录后可以进入用户中心创作、管理自己的文章。");
        settings.put(HERO_BADGE, "博客");
        settings.put(BACKGROUND_URL, "");
        settings.put(LOGO_URL, "");
        settings.put(SEO_DESCRIPTION, "");
        settings.put(SEO_KEYWORDS, "");
        settings.put(ICP_BEIAN, "");
        settings.put(FOOTER_TEXT, "");
        settings.put(ALLOW_REGISTER, "true");
        settings.put(MAIL_ENABLED, "false");
        settings.put(MAIL_HOST, "");
        settings.put(MAIL_PORT, "587");
        settings.put(MAIL_USERNAME, "");
        settings.put(MAIL_PASSWORD, "");
        settings.put(MAIL_FROM_NAME, "博客系统");
        settings.put(MAIL_SMTP_AUTH, "true");
        settings.put(MAIL_STARTTLS_ENABLE, "true");
        settings.put(MAIL_SSL_ENABLE, "false");
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
