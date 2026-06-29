package com.example.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.entity.Comment;
import com.example.blog.entity.Favorite;
import com.example.blog.entity.LikeRecord;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.mapper.FavoriteMapper;
import com.example.blog.mapper.LikeRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InteractionService {
    private final CommentMapper commentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteMapper favoriteMapper;

    public InteractionService(CommentMapper commentMapper, LikeRecordMapper likeRecordMapper, FavoriteMapper favoriteMapper) {
        this.commentMapper = commentMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteMapper = favoriteMapper;
    }

    public Page<Comment> comments(Long articleId, long current, long size) {
        return comments(articleId, current, size, null);
    }

    public Page<Comment> comments(Long articleId, long current, long size, String status) {
        return commentMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Comment>()
                .eq(articleId != null, Comment::getArticleId, articleId)
                .eq(status != null && !status.isBlank(), Comment::getStatus, status)
                .orderByDesc(Comment::getCreatedAt));
    }

    public Comment comment(Long userId, Comment comment) {
        comment.setUserId(userId);
        comment.setStatus("APPROVED");
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        return comment;
    }

    public void auditComment(Long id, String status) {
        Comment comment = commentMapper.selectById(id);
        comment.setStatus(status);
        commentMapper.updateById(comment);
    }

    public void deleteComment(Long id) {
        commentMapper.deleteById(id);
    }

    public void like(Long userId, Long articleId) {
        Long count = likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId).eq(LikeRecord::getArticleId, articleId));
        if (count == 0) {
            LikeRecord record = new LikeRecord();
            record.setUserId(userId);
            record.setArticleId(articleId);
            record.setCreatedAt(LocalDateTime.now());
            likeRecordMapper.insert(record);
        }
    }

    public void favorite(Long userId, Long articleId) {
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getArticleId, articleId));
        if (count == 0) {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setArticleId(articleId);
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteMapper.insert(favorite);
        }
    }
}
