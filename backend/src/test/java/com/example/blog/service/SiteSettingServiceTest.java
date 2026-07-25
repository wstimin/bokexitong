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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteSettingServiceTest {
    @Mock private SiteSettingMapper siteSettingMapper;

    private SiteSettingService service;

    @BeforeEach
    void setUp() {
        service = new SiteSettingService(siteSettingMapper);
        when(siteSettingMapper.selectList(null)).thenReturn(List.of());
        when(siteSettingMapper.selectOne(any())).thenReturn(null);
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
        assertEquals("#ffffff", saved.get(SiteSettingService.HERO_SUBTITLE_COLOR));
        assertEquals("dark", saved.get(SiteSettingService.HERO_TEXT_SHADOW));
    }
}
