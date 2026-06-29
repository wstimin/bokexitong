package com.example.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.dto.ArticleRequest;
import com.example.blog.entity.Article;
import com.example.blog.entity.ArticleTag;
import com.example.blog.entity.Comment;
import com.example.blog.entity.Favorite;
import com.example.blog.entity.LikeRecord;
import com.example.blog.mapper.ArticleMapper;
import com.example.blog.mapper.ArticleTagMapper;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.mapper.FavoriteMapper;
import com.example.blog.mapper.LikeRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArticleService {
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CommentMapper commentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteMapper favoriteMapper;

    public ArticleService(ArticleMapper articleMapper, ArticleTagMapper articleTagMapper, CommentMapper commentMapper,
                          LikeRecordMapper likeRecordMapper, FavoriteMapper favoriteMapper) {
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.commentMapper = commentMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteMapper = favoriteMapper;
    }

    public Page<Article> page(long current, long size, String keyword, String status, Long categoryId) {
        return articleMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Article>()
                .like(keyword != null && !keyword.isBlank(), Article::getTitle, keyword)
                .eq(status != null && !status.isBlank(), Article::getStatus, status)
                .eq(categoryId != null, Article::getCategoryId, categoryId)
                .orderByDesc(Article::getPublishedAt, Article::getCreatedAt));
    }

    public Article detail(Long id, boolean increaseView) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (increaseView) {
            article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
            articleMapper.updateById(article);
        }
        return article;
    }

    @Transactional
    public Article saveArticle(Long id, Long userId, ArticleRequest request) {
        Article article = id == null ? new Article() : detail(id, false);
        article.setUserId(article.getUserId() == null ? userId : article.getUserId());
        article.setCategoryId(request.getCategoryId());
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setCoverUrl(request.getCoverUrl());
        article.setContent(request.getContent());
        article.setContentType(request.getContentType() == null ? "MARKDOWN" : request.getContentType());
        article.setStatus(request.getStatus() == null ? "DRAFT" : request.getStatus());
        article.setUpdatedAt(LocalDateTime.now());
        if ("PUBLISHED".equals(article.getStatus()) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        if (id == null) {
            article.setViewCount(0);
            article.setLikeCount(0);
            article.setFavoriteCount(0);
            article.setCreatedAt(LocalDateTime.now());
            articleMapper.insert(article);
        } else {
            articleMapper.updateById(article);
            articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, article.getId()));
        }
        syncTags(article.getId(), request.getTagIds());
        return article;
    }

    @Transactional
    public void removeBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, ids));
        commentMapper.delete(new LambdaQueryWrapper<Comment>().in(Comment::getArticleId, ids));
        likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>().in(LikeRecord::getArticleId, ids));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().in(Favorite::getArticleId, ids));
        articleMapper.deleteBatchIds(ids);
    }

    private void syncTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null) {
            return;
        }
        for (Long tagId : tagIds) {
            ArticleTag relation = new ArticleTag();
            relation.setArticleId(articleId);
            relation.setTagId(tagId);
            articleTagMapper.insert(relation);
        }
    }
}
