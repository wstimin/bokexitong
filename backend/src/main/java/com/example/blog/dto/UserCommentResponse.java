package com.example.blog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCommentResponse {
    private Long id;
    private Long articleId;
    private String articleTitle;
    private String articleStatus;
    private String content;
    private String status;
    private String reviewReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
