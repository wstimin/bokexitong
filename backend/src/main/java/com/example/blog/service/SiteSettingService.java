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
    private static final long CACHE_TTL_MILLIS = 30_000L;
    public static final String SITE_NAME = "siteName";
    public static final String HERO_TITLE = "heroTitle";
    public static final String HERO_SUBTITLE = "heroSubtitle";
    public static final String HERO_BADGE = "heroBadge";
    public static final String HERO_TITLE_COLOR = "heroTitleColor";
    public static final String HERO_SUBTITLE_COLOR = "heroSubtitleColor";
    public static final String HERO_TEXT_SHADOW = "heroTextShadow";
    public static final String BACKGROUND_URL = "backgroundUrl";
    public static final String LOGO_URL = "logoUrl";
    public static final String SEO_DESCRIPTION = "seoDescription";
    public static final String SEO_KEYWORDS = "seoKeywords";
    public static final String ICP_BEIAN = "icpBeian";
    public static final String FOOTER_TEXT = "footerText";
    public static final String CONTACT_HTML = "contactHtml";
    public static final String ALLOW_REGISTER = "allowRegister";
    public static final String ADMIN_LOGIN_PATH = "adminLoginPath";
    public static final String FORBIDDEN_WORDS = "forbiddenWords";
    public static final String MAIL_ENABLED = "mailEnabled";
    public static final String MAIL_HOST = "mailHost";
    public static final String MAIL_PORT = "mailPort";
    public static final String MAIL_USERNAME = "mailUsername";
    public static final String MAIL_PASSWORD = "mailPassword";
    public static final String MAIL_FROM_NAME = "mailFromName";
    public static final String MAIL_SMTP_AUTH = "mailSmtpAuth";
    public static final String MAIL_STARTTLS_ENABLE = "mailStarttlsEnable";
    public static final String MAIL_SSL_ENABLE = "mailSslEnable";
    public static final String INSTALLATION_COMPLETE = "installationComplete";

    private static final Set<String> PUBLIC_KEYS = Set.of(
            SITE_NAME, HERO_TITLE, HERO_SUBTITLE, HERO_BADGE, HERO_TITLE_COLOR, HERO_SUBTITLE_COLOR,
            HERO_TEXT_SHADOW, BACKGROUND_URL, LOGO_URL,
            SEO_DESCRIPTION, SEO_KEYWORDS, ICP_BEIAN, FOOTER_TEXT, CONTACT_HTML, ALLOW_REGISTER, ADMIN_LOGIN_PATH
    );
    private static final Set<String> SITE_FORM_KEYS = Set.of(
            SITE_NAME, HERO_TITLE, HERO_SUBTITLE, HERO_BADGE, HERO_TITLE_COLOR, HERO_SUBTITLE_COLOR,
            HERO_TEXT_SHADOW, BACKGROUND_URL, LOGO_URL,
            SEO_DESCRIPTION, SEO_KEYWORDS, ICP_BEIAN, FOOTER_TEXT, CONTACT_HTML, ALLOW_REGISTER, ADMIN_LOGIN_PATH,
            FORBIDDEN_WORDS
    );
    private static final Set<String> MAIL_FORM_KEYS = Set.of(
            MAIL_ENABLED, MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM_NAME,
            MAIL_SMTP_AUTH, MAIL_STARTTLS_ENABLE, MAIL_SSL_ENABLE
    );
    private static final Set<String> REQUIRED_TEXT_KEYS = Set.of(
            SITE_NAME, HERO_TITLE, HERO_SUBTITLE, HERO_BADGE, MAIL_PORT, MAIL_FROM_NAME, ADMIN_LOGIN_PATH
    );

    private final SiteSettingMapper siteSettingMapper;
    private volatile List<SiteSetting> cachedRows = List.of();
    private volatile long cacheExpiresAt;

    public SiteSettingService(SiteSettingMapper siteSettingMapper) {
        this.siteSettingMapper = siteSettingMapper;
    }

    public Map<String, String> publicSettings() {
        return loadSettings(PUBLIC_KEYS, false);
    }

    public Map<String, String> adminSettings() {
        return loadSettings(defaults().keySet(), true);
    }

    public Map<String, String> adminSiteSettings() {
        return loadSettings(SITE_FORM_KEYS, false);
    }

    public Map<String, String> adminMailSettings() {
        return loadSettings(MAIL_FORM_KEYS, true);
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
        List<SiteSetting> rows = settingRows();
        for (SiteSetting row : rows) {
            if (settings.containsKey(row.getSettingKey()) && row.getSettingValue() != null) {
                String value = normalizeAppearanceSetting(row.getSettingKey(), row.getSettingValue(), source.get(row.getSettingKey()));
                settings.put(row.getSettingKey(), shouldUseDefault(row.getSettingKey(), value) ? source.get(row.getSettingKey()) : value);
            }
        }
        if (hideSecret && settings.containsKey(MAIL_PASSWORD)) {
            settings.put(MAIL_PASSWORD, "");
        }
        return settings;
    }

    public Map<String, String> save(Map<String, String> payload) {
        Map<String, String> next = mailSettings();
        Map<String, String> current = mailSettings();
        for (String key : next.keySet()) {
            if (payload.containsKey(key)) {
                if (MAIL_PASSWORD.equals(key) && clean(payload.get(key), "").isEmpty()) {
                    next.put(key, current.getOrDefault(key, ""));
                } else if (ALLOW_REGISTER.equals(key) || MAIL_ENABLED.equals(key) || MAIL_SMTP_AUTH.equals(key)
                        || MAIL_STARTTLS_ENABLE.equals(key) || MAIL_SSL_ENABLE.equals(key)) {
                    next.put(key, normalizeBoolean(payload.get(key), next.get(key)));
                } else if (ADMIN_LOGIN_PATH.equals(key)) {
                    next.put(key, normalizeAdminLoginPath(payload.get(key), next.get(key)));
                } else if (HERO_TITLE_COLOR.equals(key) || HERO_SUBTITLE_COLOR.equals(key)) {
                    next.put(key, normalizeColor(payload.get(key), next.get(key)));
                } else if (HERO_TEXT_SHADOW.equals(key)) {
                    next.put(key, normalizeHeroTextShadow(payload.get(key), next.get(key)));
                } else {
                    next.put(key, clean(payload.get(key), next.get(key), REQUIRED_TEXT_KEYS.contains(key)));
                }
            } else if (MAIL_PASSWORD.equals(key)) {
                next.put(key, current.getOrDefault(key, ""));
            }
            upsert(key, next.get(key));
        }
        return adminSettings();
    }

    public Map<String, String> saveSite(Map<String, String> payload) {
        Map<String, String> next = adminSiteSettings();
        Map<String, String> filtered = filter(payload, SITE_FORM_KEYS);
        for (String key : next.keySet()) {
            if (filtered.containsKey(key)) {
                if (ALLOW_REGISTER.equals(key)) {
                    next.put(key, normalizeBoolean(filtered.get(key), next.get(key)));
                } else if (ADMIN_LOGIN_PATH.equals(key)) {
                    next.put(key, normalizeAdminLoginPath(filtered.get(key), next.get(key)));
                } else if (HERO_TITLE_COLOR.equals(key) || HERO_SUBTITLE_COLOR.equals(key)) {
                    next.put(key, normalizeColor(filtered.get(key), next.get(key)));
                } else if (HERO_TEXT_SHADOW.equals(key)) {
                    next.put(key, normalizeHeroTextShadow(filtered.get(key), next.get(key)));
                } else {
                    next.put(key, clean(filtered.get(key), next.get(key), REQUIRED_TEXT_KEYS.contains(key)));
                }
            }
            upsert(key, next.get(key));
        }
        return adminSiteSettings();
    }

    public String saveAdminLoginPath(String value) {
        String normalized = requireValidAdminLoginPath(value);
        upsert(ADMIN_LOGIN_PATH, normalized);
        return normalized;
    }

    public Map<String, String> saveMail(Map<String, String> payload) {
        save(filter(payload, MAIL_FORM_KEYS));
        return adminMailSettings();
    }

    public boolean allowRegister() {
        return "true".equalsIgnoreCase(publicSettings().get(ALLOW_REGISTER));
    }

    public boolean isCurrentAdminLoginPath(String value) {
        String candidate = value == null ? "" : value.trim();
        return !candidate.isEmpty() && candidate.equals(publicSettings().get(ADMIN_LOGIN_PATH));
    }

    public boolean isInstalled() {
        try {
            SiteSetting row = siteSettingMapper.selectOne(new LambdaQueryWrapper<SiteSetting>()
                    .eq(SiteSetting::getSettingKey, INSTALLATION_COMPLETE)
                    .last("LIMIT 1"));
            return row != null && "true".equalsIgnoreCase(String.valueOf(row.getSettingValue()));
        } catch (Exception ignored) {
            return false;
        }
    }

    public void markInstalled() {
        upsert(INSTALLATION_COMPLETE, "true");
    }

    public List<String> forbiddenWords() {
        return parseList(adminSiteSettings().get(FORBIDDEN_WORDS));
    }

    private Map<String, String> defaults() {
        Map<String, String> settings = new HashMap<>();
        settings.put(SITE_NAME, "博客系统");
        settings.put(HERO_TITLE, "博客系统");
        settings.put(HERO_SUBTITLE, "用卡片浏览公开文章，点开后再阅读完整内容。登录后可以进入用户中心创作、管理自己的文章。");
        settings.put(HERO_BADGE, "博客");
        settings.put(HERO_TITLE_COLOR, "#f7f5f2");
        settings.put(HERO_SUBTITLE_COLOR, "#f0ebe3");
        settings.put(HERO_TEXT_SHADOW, "dark");
        settings.put(BACKGROUND_URL, "");
        settings.put(LOGO_URL, "");
        settings.put(SEO_DESCRIPTION, "");
        settings.put(SEO_KEYWORDS, "");
        settings.put(ICP_BEIAN, "");
        settings.put(FOOTER_TEXT, "");
        settings.put(CONTACT_HTML, "");
        settings.put(ALLOW_REGISTER, "true");
        settings.put(ADMIN_LOGIN_PATH, "/admin/login");
        settings.put(FORBIDDEN_WORDS, "赌博,色情,毒品,诈骗");
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
        return clean(value, fallback, true);
    }

    private String clean(String value, String fallback, boolean required) {
        String trimmed = value == null ? "" : value.trim();
        return required && trimmed.isEmpty() ? fallback : trimmed;
    }

    private String normalizeBoolean(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return trimmed.toLowerCase();
        }
        return fallback;
    }

    private String normalizeAppearanceSetting(String key, String value, String fallback) {
        if (HERO_TITLE_COLOR.equals(key) || HERO_SUBTITLE_COLOR.equals(key)) {
            return normalizeColor(value, fallback);
        }
        if (HERO_TEXT_SHADOW.equals(key)) {
            return normalizeHeroTextShadow(value, fallback);
        }
        return value;
    }

    private String normalizeColor(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.matches("^#[0-9a-fA-F]{6}$") ? trimmed.toLowerCase() : fallback;
    }

    private String normalizeHeroTextShadow(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim().toLowerCase();
        return Set.of("none", "dark", "light").contains(trimmed) ? trimmed : fallback;
    }

    private String normalizeAdminLoginPath(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return fallback == null || fallback.isBlank() ? "/admin/login" : fallback;
        }
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        if ("/".equals(trimmed) || "/login".equals(trimmed) || "/user".equals(trimmed) || "/article".equals(trimmed)) {
            return fallback == null || fallback.isBlank() ? "/admin/login" : fallback;
        }
        if (trimmed.length() > 80 || !trimmed.matches("/[A-Za-z0-9/_-]+")) {
            return fallback == null || fallback.isBlank() ? "/admin/login" : fallback;
        }
        return trimmed;
    }

    private String requireValidAdminLoginPath(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("请填写后台登录路径");
        }
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        while (trimmed.length() > 1 && trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.length() > 80 || !trimmed.matches("/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*")) {
            throw new IllegalArgumentException("后台登录路径只能包含字母、数字、横线、下划线和斜杠，且不能超过 80 个字符");
        }
        Set<String> reservedPrefixes = Set.of("/api", "/uploads", "/install", "/login", "/user", "/article", "/archive", "/about", "/create");
        for (String prefix : reservedPrefixes) {
            if (trimmed.equals(prefix) || trimmed.startsWith(prefix + "/")) {
                throw new IllegalArgumentException("该路径与系统页面冲突，请更换后台登录路径");
            }
        }
        if ((trimmed.equals("/admin") || trimmed.startsWith("/admin/")) && !"/admin/login".equals(trimmed)) {
            throw new IllegalArgumentException("自定义登录路径不能使用 /admin 管理路由前缀");
        }
        return trimmed;
    }

    private List<String> parseList(String config) {
        if (config == null || config.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(config.split("[,，;；\\r\\n]+"))
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .distinct()
                .toList();
    }

    private Map<String, String> filter(Map<String, String> payload, Set<String> allowedKeys) {
        Map<String, String> filtered = new HashMap<>();
        for (String key : allowedKeys) {
            if (payload.containsKey(key)) {
                filtered.put(key, payload.get(key));
            }
        }
        return filtered;
    }

    private boolean shouldUseDefault(String key, String value) {
        return Set.of(SITE_NAME, HERO_TITLE, HERO_SUBTITLE, HERO_BADGE, MAIL_FROM_NAME).contains(key)
                && looksLikeMojibake(value);
    }

    private boolean looksLikeMojibake(String value) {
        if (value == null) {
            return false;
        }
        return value.contains("�")
                || value.contains("å")
                || value.contains("ã")
                || value.contains("Ã")
                || value.contains("Â")
                || value.contains("鍗氬")
                || value.contains("绯荤粺")
                || value.contains("杩欐")
                || value.contains("涓€")
                || value.contains("銆")
                || value.contains("鈥")
                || value.codePoints().anyMatch(codePoint -> codePoint >= 0xE000 && codePoint <= 0xF8FF)
                || value.contains("¢")
                || value.contains("®")
                || value.contains("»")
                || value.contains("¿");
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
        invalidateCache();
    }

    private List<SiteSetting> settingRows() {
        long now = System.currentTimeMillis();
        if (now < cacheExpiresAt) {
            return cachedRows;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now < cacheExpiresAt) {
                return cachedRows;
            }
            try {
                cachedRows = List.copyOf(siteSettingMapper.selectList(null));
                cacheExpiresAt = now + CACHE_TTL_MILLIS;
            } catch (Exception ignored) {
                cachedRows = List.of();
                cacheExpiresAt = now + 2_000L;
            }
            return cachedRows;
        }
    }

    private void invalidateCache() {
        cacheExpiresAt = 0L;
    }
}
