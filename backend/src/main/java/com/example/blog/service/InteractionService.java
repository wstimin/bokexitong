package com.example.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.dto.InteractionResponse;
import com.example.blog.dto.UserCommentResponse;
import com.example.blog.entity.Article;
import com.example.blog.entity.Comment;
import com.example.blog.entity.Favorite;
import com.example.blog.entity.LikeRecord;
import com.example.blog.mapper.ArticleMapper;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.mapper.FavoriteMapper;
import com.example.blog.mapper.LikeRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InteractionService {
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteMapper favoriteMapper;

    public InteractionService(ArticleMapper articleMapper, CommentMapper commentMapper,
                              LikeRecordMapper likeRecordMapper, FavoriteMapper favoriteMapper) {
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteMapper = favoriteMapper;
    }

    public Page<Comment> comments(Long articleId, long current, long size) {
        return comments(articleId, current, size, "APPROVED");
    }

    public Page<Comment> comments(Long articleId, long current, long size, String status) {
        return commentMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Comment>()
                .eq(articleId != null, Comment::getArticleId, articleId)
                .eq(status != null && !status.isBlank(), Comment::getStatus, status)
                .orderByDesc(Comment::getCreatedAt));
    }

    public Page<UserCommentResponse> userComments(Long userId, long current, long size) {
        Page<Comment> commentPage = commentMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .orderByDesc(Comment::getCreatedAt));
        Page<UserCommentResponse> responsePage = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
        responsePage.setRecords(toUserCommentResponses(commentPage.getRecords()));
        return responsePage;
    }

    public Comment comment(Long userId, Comment comment) {
        Article article = ensurePublishedArticle(comment.getArticleId());
        String content = clean(comment.getContent());
        if (content == null) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("评论内容不能超过 1000 字");
        }
        comment.setUserId(userId);
        comment.setArticleId(article.getId());
        comment.setParentId(comment.getParentId() == null || comment.getParentId() < 0 ? 0 : comment.getParentId());
        comment.setContent(content);
        comment.setStatus("PENDING");
        comment.setReviewReason(null);
        comment.setReviewedAt(null);
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        return comment;
    }

    public void auditComment(Long id, String status, String reason) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new IllegalArgumentException("Comment does not exist");
        }
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalArgumentException("Unsupported comment status");
        }
        comment.setStatus(status);
        comment.setReviewReason("REJECTED".equals(status) ? clean(reason) : null);
        comment.setReviewedAt(LocalDateTime.now());
        commentMapper.updateById(comment);
    }

    public void deleteComment(Long id) {
        commentMapper.deleteById(id);
    }

    @Transactional
    public InteractionResponse like(Long userId, Long articleId) {
        ensurePublishedArticle(articleId);
        LikeRecord existing = likeRecordMapper.selectOne(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId).eq(LikeRecord::getArticleId, articleId));
        boolean active;
        if (existing == null) {
            LikeRecord record = new LikeRecord();
            record.setUserId(userId);
            record.setArticleId(articleId);
            record.setCreatedAt(LocalDateTime.now());
            likeRecordMapper.insert(record);
            active = true;
        } else {
            likeRecordMapper.deleteById(existing.getId());
            active = false;
        }
        return syncLikeCount(articleId, active);
    }

    @Transactional
    public InteractionResponse favorite(Long userId, Long articleId) {
        ensurePublishedArticle(articleId);
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getArticleId, articleId));
        boolean active;
        if (existing == null) {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setArticleId(articleId);
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteMapper.insert(favorite);
            active = true;
        } else {
            favoriteMapper.deleteById(existing.getId());
            active = false;
        }
        return syncFavoriteCount(articleId, active);
    }

    private InteractionResponse syncLikeCount(Long articleId, boolean active) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("Article does not exist");
        }
        int count = likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getArticleId, articleId)).intValue();
        article.setLikeCount(count);
        articleMapper.updateById(article);
        return new InteractionResponse(active, count);
    }

    private InteractionResponse syncFavoriteCount(Long articleId, boolean active) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("Article does not exist");
        }
        int count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getArticleId, articleId)).intValue();
        article.setFavoriteCount(count);
        articleMapper.updateById(article);
        return new InteractionResponse(active, count);
    }

    private Article ensurePublishedArticle(Long articleId) {
        if (articleId == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null || (article.getDeleted() != null && article.getDeleted() == 1)) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (!"PUBLISHED".equals(article.getStatus())) {
            throw new IllegalArgumentException("文章暂不可互动");
        }
        return article;
    }

    private List<UserCommentResponse> toUserCommentResponses(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> articleIds = comments.stream().map(Comment::getArticleId).distinct().toList();
        Map<Long, Article> articles = articleMapper.selectBatchIds(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));
        return comments.stream().map(comment -> {
            Article article = articles.get(comment.getArticleId());
            UserCommentResponse response = new UserCommentResponse();
            response.setId(comment.getId());
            response.setArticleId(comment.getArticleId());
            response.setArticleTitle(article == null ? "文章已删除" : article.getTitle());
            response.setArticleStatus(article == null ? "DELETED" : article.getStatus());
            response.setContent(comment.getContent());
            response.setStatus(comment.getStatus());
            response.setReviewReason(comment.getReviewReason());
            response.setReviewedAt(comment.getReviewedAt());
            response.setCreatedAt(comment.getCreatedAt());
            return response;
        }).toList();
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
