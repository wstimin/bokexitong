package com.example.blog.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardStats {
    private Long articleCount;
    private Long userCount;
    private Long commentCount;
    private Long imageCount;
    private Long likeCount;
    private Long favoriteCount;
    private Long pendingArticleCount;
    private Long publishedArticleCount;
    private Long rejectedArticleCount;
    private Long offlineArticleCount;
    private Long pendingCommentCount;
    private Long approvedCommentCount;
    private Long rejectedCommentCount;
    private List<Map<String, Object>> publishTrend;
    private List<Map<String, Object>> categoryPie;
    private List<Map<String, Object>> statusPie;
}
