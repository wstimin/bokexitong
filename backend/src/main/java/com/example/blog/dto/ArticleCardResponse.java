package com.example.blog.dto;

import com.example.blog.entity.Tag;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleCardResponse {
    private Long id;
    private String title;
    private String summary;
    private String coverUrl;
    private Long categoryId;
    private String categoryName;
    private Long authorId;
    private String authorName;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Tag> tags;
}
