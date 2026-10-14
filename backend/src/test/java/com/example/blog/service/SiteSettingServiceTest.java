package com.example.blog.service;

import com.example.blog.entity.SiteSetting;
import com.example.blog.mapper.SiteSettingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SiteSettingServiceTest {
    @Mock private SiteSettingMapper siteSettingMapper;

    private SiteSettingService service;

    @BeforeEach
    void setUp() {
        service = new SiteSettingService(siteSettingMapper);
        lenient().when(siteSettingMapper.selectList(null)).thenReturn(List.of());
        lenient().when(siteSettingMapper.selectOne(any())).thenReturn(null);
    }

    @Test
    void saveSite_shouldNormalizeHeroAppearanceSettings() {
        service.saveSite(Map.of(
                SiteSettingService.HERO_TITLE_COLOR, "#A1B2C3",
                SiteSettingService.HERO_SUBTITLE_COLOR, "red",
                SiteSettingService.HERO_TEXT_SHADOW, "custom"
        ));

        ArgumentCaptor<SiteSetting> captor = ArgumentCaptor.forClass(SiteSetting.class);
        verify(siteSettingMapper, atLeastOnce()).insert(captor.capture());
        Map<String, String> saved = captor.getAllValues().stream()
                .collect(java.util.stream.Collectors.toMap(SiteSetting::getSettingKey, SiteSetting::getSettingValue));

        assertEquals("#a1b2c3", saved.get(SiteSettingService.HERO_TITLE_COLOR));
        assertEquals("#f0ebe3", saved.get(SiteSettingService.HERO_SUBTITLE_COLOR));
        assertEquals("dark", saved.get(SiteSettingService.HERO_TEXT_SHADOW));
    }

    @Test
    void saveAdminLoginPath_shouldNormalizeAndPersistDedicatedSetting() {
        String savedPath = service.saveAdminLoginPath("shiyeotimin/login/");

        ArgumentCaptor<SiteSetting> captor = ArgumentCaptor.forClass(SiteSetting.class);
        verify(siteSettingMapper).insert(captor.capture());
        assertEquals("/shiyeotimin/login", savedPath);
        assertEquals(SiteSettingService.ADMIN_LOGIN_PATH, captor.getValue().getSettingKey());
        assertEquals("/shiyeotimin/login", captor.getValue().getSettingValue());
    }

    @Test
    void saveAdminLoginPath_shouldRejectRoutesOwnedByTheApplication() {
        assertThrows(IllegalArgumentException.class, () -> service.saveAdminLoginPath("/admin/settings"));
        assertThrows(IllegalArgumentException.class, () -> service.saveAdminLoginPath("/api/admin/login"));
        assertThrows(IllegalArgumentException.class, () -> service.saveAdminLoginPath("/about"));
        assertThrows(IllegalArgumentException.class, () -> service.saveAdminLoginPath("/manage-login"));
        assertThrows(IllegalArgumentException.class, () -> service.saveAdminLoginPath("/one/two/three/login"));
    }

    @Test
    void isCurrentAdminLoginPath_shouldRequireAnExactMatch() {
        assertTrue(service.isCurrentAdminLoginPath("/admin/login"));
        assertFalse(service.isCurrentAdminLoginPath("/admin/login/"));
        assertFalse(service.isCurrentAdminLoginPath("/old-admin/login"));
        assertFalse(service.isCurrentAdminLoginPath(null));
    }
}
