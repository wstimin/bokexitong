package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("like_record")
public class LikeRecord {
    private Long id;
    private Long userId;
    private Long articleId;
    private LocalDateTime createdAt;
}
