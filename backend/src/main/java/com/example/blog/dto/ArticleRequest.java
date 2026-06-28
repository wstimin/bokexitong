package com.example.blog.dto;

import lombok.Data;

import java.util.List;

@Data
public class ArticleRequest {
    private Long categoryId;
    private String title;
    private String summary;
    private String coverUrl;
    private String content;
    private String contentType;
    private String status;
    private List<Long> tagIds;
}
