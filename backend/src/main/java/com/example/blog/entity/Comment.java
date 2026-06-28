package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    private Long id;
    private Long articleId;
    private Long userId;
    private Long parentId;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
