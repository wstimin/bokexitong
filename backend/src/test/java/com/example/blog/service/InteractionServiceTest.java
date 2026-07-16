package com.example.blog.service;

import com.example.blog.entity.Article;
import com.example.blog.entity.Comment;
import com.example.blog.mapper.ArticleMapper;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.mapper.FavoriteMapper;
import com.example.blog.mapper.LikeRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

    @Mock private ArticleMapper articleMapper;
    @Mock private CommentMapper commentMapper;
    @Mock private LikeRecordMapper likeRecordMapper;
    @Mock private FavoriteMapper favoriteMapper;
    @Mock private SiteSettingService siteSettingService;

    private InteractionService service;

    @BeforeEach
    void setUp() {
        service = new InteractionService(articleMapper, commentMapper, likeRecordMapper, favoriteMapper, siteSettingService);
    }

    @Test
    void comment_shouldApproveDirectly_whenContentIsClean() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus("PUBLISHED");
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(siteSettingService.forbiddenWords()).thenReturn(List.of("赌博"));
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);

        Comment result = service.comment(7L, buildComment("正常评论内容"));

        assertEquals("APPROVED", result.getStatus());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    void comment_shouldReject_whenForbiddenWordMatched() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus("PUBLISHED");
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(siteSettingService.forbiddenWords()).thenReturn(List.of("赌博"));

        Comment result = service.comment(7L, buildComment("这里有赌博相关内容"));

        assertEquals("REJECTED", result.getStatus());
        assertEquals("命中违禁词：赌博", result.getReviewReason());
        assertNotNull(result.getReviewedAt());
    }

    private Comment buildComment(String content) {
        Comment comment = new Comment();
        comment.setArticleId(1L);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }
}
