package com.example.blog.dto;

import lombok.Data;

@Data
public class ArticleDetailResponse extends ArticleCardResponse {
    private String content;
    private String contentType;
}
