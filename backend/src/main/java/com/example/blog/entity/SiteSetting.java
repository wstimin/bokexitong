package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_setting")
public class SiteSetting {
    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;
    private LocalDateTime updatedAt;
}
