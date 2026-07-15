package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {
    private Long id;
    private Long userId;
    private Long categoryId;
    private String title;
    private String summary;
    private String coverUrl;
    private String content;
    private String contentType;
    private String status;
    private Integer recommended;
    private Integer recommendSort;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private LocalDateTime publishedAt;
    private String reviewReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
