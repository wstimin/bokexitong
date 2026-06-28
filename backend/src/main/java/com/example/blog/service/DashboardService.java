package com.example.blog.service;

import com.example.blog.dto.DashboardStats;
import com.example.blog.mapper.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    private final ArticleMapper articleMapper;
    private final BlogUserMapper userMapper;
    private final CommentMapper commentMapper;
    private final ImageResourceMapper imageResourceMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteMapper favoriteMapper;

    public DashboardService(ArticleMapper articleMapper, BlogUserMapper userMapper, CommentMapper commentMapper,
                            ImageResourceMapper imageResourceMapper, LikeRecordMapper likeRecordMapper,
                            FavoriteMapper favoriteMapper) {
        this.articleMapper = articleMapper;
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
        this.imageResourceMapper = imageResourceMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteMapper = favoriteMapper;
    }

    public DashboardStats stats() {
        return DashboardStats.builder()
                .articleCount(articleMapper.selectCount(null))
                .userCount(userMapper.selectCount(null))
                .commentCount(commentMapper.selectCount(null))
                .imageCount(imageResourceMapper.selectCount(null))
                .likeCount(likeRecordMapper.selectCount(null))
                .favoriteCount(favoriteMapper.selectCount(null))
                .publishTrend(List.of(
                        Map.of("date", "周一", "count", 3),
                        Map.of("date", "周二", "count", 5),
                        Map.of("date", "周三", "count", 2),
                        Map.of("date", "周四", "count", 7),
                        Map.of("date", "周五", "count", 4)
                ))
                .categoryPie(List.of(
                        Map.of("name", "日常随笔", "value", 10),
                        Map.of("name", "技术笔记", "value", 8),
                        Map.of("name", "动漫杂谈", "value", 6)
                ))
                .build();
    }
}
