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
    private List<Map<String, Object>> publishTrend;
    private List<Map<String, Object>> categoryPie;
}
