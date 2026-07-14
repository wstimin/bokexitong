package com.example.blog.service;

import com.example.blog.dto.DashboardStats;
import com.example.blog.entity.Article;
import com.example.blog.entity.Category;
import com.example.blog.entity.Comment;
import com.example.blog.mapper.ArticleMapper;
import com.example.blog.mapper.BlogUserMapper;
import com.example.blog.mapper.CategoryMapper;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.mapper.FavoriteMapper;
import com.example.blog.mapper.ImageResourceMapper;
import com.example.blog.mapper.LikeRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final ArticleMapper articleMapper;
    private final BlogUserMapper userMapper;
    private final CommentMapper commentMapper;
    private final ImageResourceMapper imageResourceMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteMapper favoriteMapper;
    private final CategoryMapper categoryMapper;

    public DashboardService(ArticleMapper articleMapper, BlogUserMapper userMapper, CommentMapper commentMapper,
                            ImageResourceMapper imageResourceMapper, LikeRecordMapper likeRecordMapper,
                            FavoriteMapper favoriteMapper, CategoryMapper categoryMapper) {
        this.articleMapper = articleMapper;
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
        this.imageResourceMapper = imageResourceMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteMapper = favoriteMapper;
        this.categoryMapper = categoryMapper;
    }

    public DashboardStats stats() {
        List<Article> articles = articleMapper.selectList(null);
        List<Comment> comments = commentMapper.selectList(null);
        Map<String, Long> articleStatusCounts = countBy(articles, Article::getStatus);
        Map<String, Long> commentStatusCounts = countBy(comments, Comment::getStatus);

        return DashboardStats.builder()
                .articleCount((long) articles.size())
                .userCount(userMapper.selectCount(null))
                .commentCount((long) comments.size())
                .imageCount(imageResourceMapper.selectCount(null))
                .likeCount(likeRecordMapper.selectCount(null))
                .favoriteCount(favoriteMapper.selectCount(null))
                .pendingArticleCount(articleStatusCounts.getOrDefault("PENDING", 0L))
                .publishedArticleCount(articleStatusCounts.getOrDefault("PUBLISHED", 0L))
                .rejectedArticleCount(articleStatusCounts.getOrDefault("REJECTED", 0L))
                .offlineArticleCount(articleStatusCounts.getOrDefault("OFFLINE", 0L))
                .pendingCommentCount(commentStatusCounts.getOrDefault("PENDING", 0L))
                .approvedCommentCount(commentStatusCounts.getOrDefault("APPROVED", 0L))
                .rejectedCommentCount(commentStatusCounts.getOrDefault("REJECTED", 0L))
                .publishTrend(buildPublishTrend(articles))
                .categoryPie(buildCategoryPie(articles))
                .statusPie(buildStatusPie(articleStatusCounts))
                .build();
    }

    private <T> Map<String, Long> countBy(List<T> rows, Function<T, String> classifier) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        return rows.stream()
                .map(classifier)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private List<Map<String, Object>> buildPublishTrend(List<Article> articles) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> counts = articles.stream()
                .filter(article -> "PUBLISHED".equals(article.getStatus()))
                .map(article -> article.getPublishedAt() == null ? article.getCreatedAt() : article.getPublishedAt())
                .filter(Objects::nonNull)
                .map(dateTime -> dateTime.toLocalDate())
                .filter(date -> !date.isBefore(today.minusDays(6)) && !date.isAfter(today))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            trend.add(Map.of("date", date.toString(), "count", counts.getOrDefault(date, 0L)));
        }
        return trend;
    }

    private List<Map<String, Object>> buildCategoryPie(List<Article> articles) {
        Map<Long, Long> counts = articles.stream()
                .filter(article -> "PUBLISHED".equals(article.getStatus()))
                .map(Article::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        if (counts.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Category> categories = categoryMapper.selectBatchIds(counts.keySet()).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        return counts.entrySet().stream()
                .map(entry -> {
                    Category category = categories.get(entry.getKey());
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", category == null ? "未分类" : category.getName());
                    item.put("value", entry.getValue());
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> buildStatusPie(Map<String, Long> statusCounts) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("DRAFT", "草稿");
        labels.put("PENDING", "待审核");
        labels.put("PUBLISHED", "已发布");
        labels.put("REJECTED", "已驳回");
        labels.put("OFFLINE", "已下架");

        return labels.entrySet().stream()
                .map(entry -> Map.<String, Object>of("name", entry.getValue(), "value", statusCounts.getOrDefault(entry.getKey(), 0L)))
                .toList();
    }
}
