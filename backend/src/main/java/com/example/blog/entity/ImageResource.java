package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("image_resource")
public class ImageResource {
    private Long id;
    private String title;
    private String url;
    private String type;
    private String description;
    private Integer sort;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
