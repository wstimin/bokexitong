package com.example.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.dto.ArticleRequest;
import com.example.blog.entity.Article;
import com.example.blog.mapper.ArticleMapper;
import com.example.blog.mapper.ArticleTagMapper;
import com.example.blog.mapper.BlogUserMapper;
import com.example.blog.mapper.CategoryMapper;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.mapper.FavoriteMapper;
import com.example.blog.mapper.LikeRecordMapper;
import com.example.blog.mapper.TagMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock private ArticleMapper articleMapper;
    @Mock private ArticleTagMapper articleTagMapper;
    @Mock private CommentMapper commentMapper;
    @Mock private LikeRecordMapper likeRecordMapper;
    @Mock private FavoriteMapper favoriteMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private TagMapper tagMapper;
    @Mock private BlogUserMapper blogUserMapper;
    @Mock private SiteSettingService siteSettingService;
    @Mock private RichTextMediaMigrationService richTextMediaMigrationService;

    private ArticleService service;

    @BeforeEach
    void setUp() {
        service = new ArticleService(
                articleMapper,
                articleTagMapper,
                commentMapper,
                likeRecordMapper,
                favoriteMapper,
                categoryMapper,
                tagMapper,
                blogUserMapper,
                siteSettingService,
                richTextMediaMigrationService
        );
        lenient().when(richTextMediaMigrationService.migrateEmbeddedImages(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void saveArticle_shouldPublishDirectly_whenContentIsClean() {
        ArticleRequest request = baseRequest("\u53d1\u5e03\u6807\u9898", "\u6b63\u5e38\u6458\u8981", "<p>\u6b63\u6587\u5185\u5bb9\u5f88\u6b63\u5e38</p>");
        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        when(articleMapper.insert(captor.capture())).thenReturn(1);

        Article article = service.saveArticle(null, 7L, request);

        assertEquals("PUBLISHED", article.getStatus());
        assertNull(article.getReviewReason());
        assertNotNull(article.getReviewedAt());
        assertNotNull(article.getPublishedAt());
        assertEquals("PUBLISHED", captor.getValue().getStatus());
        assertEquals("\u53d1\u5e03\u6807\u9898", captor.getValue().getTitle());
        verifyNoInteractions(articleTagMapper);
    }

    @Test
    void saveArticle_shouldSendToReview_whenForbiddenWordMatched() {
        ArticleRequest request = baseRequest("\u53d1\u5e03\u6807\u9898", "\u6b63\u5e38\u6458\u8981", "<p>\u8fd9\u91cc\u5305\u542b\u8d4c\u535a\u76f8\u5173\u5185\u5bb9</p>");
        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        when(siteSettingService.forbiddenWords()).thenReturn(List.of("\u8d4c\u535a"));
        when(articleMapper.insert(captor.capture())).thenReturn(1);

        Article article = service.saveArticle(null, 7L, request);

        assertEquals("REJECTED", article.getStatus());
        assertEquals("\u547d\u4e2d\u8fdd\u7981\u8bcd\uff1a\u8d4c\u535a", article.getReviewReason());
        assertNotNull(article.getReviewedAt());
        assertNull(article.getPublishedAt());
        assertEquals("REJECTED", captor.getValue().getStatus());
        assertEquals("\u547d\u4e2d\u8fdd\u7981\u8bcd\uff1a\u8d4c\u535a", captor.getValue().getReviewReason());
        verifyNoInteractions(articleTagMapper);
    }

    @Test
    void recommendedCards_shouldStayEmpty_whenNoArticleIsRecommended() {
        Page<Article> emptyPage = new Page<>(1, 6);
        emptyPage.setRecords(List.of());
        when(articleMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

        assertTrue(service.recommendedCards(6).isEmpty());

        verify(articleMapper, times(1)).selectPage(any(Page.class), any());
        verifyNoInteractions(categoryMapper, tagMapper, blogUserMapper);
    }

    @Test
    void updateRecommendationByAdmin_shouldStoreOneForRecommendedAndZeroForNotRecommended() {
        Article article = new Article();
        article.setId(12L);
        article.setStatus("PUBLISHED");
        when(articleMapper.selectById(12L)).thenReturn(article);

        service.updateRecommendationByAdmin(12L, true, 3);
        assertEquals(1, article.getRecommended());
        assertEquals(3, article.getRecommendSort());

        service.updateRecommendationByAdmin(12L, false, 3);
        assertEquals(0, article.getRecommended());
        assertEquals(0, article.getRecommendSort());
    }

    @Test
    void updateRecommendationByAdmin_shouldClampNegativeSortToZero() {
        Article article = new Article();
        article.setId(13L);
        article.setStatus("PUBLISHED");
        when(articleMapper.selectById(13L)).thenReturn(article);

        service.updateRecommendationByAdmin(13L, true, -2);

        assertEquals(1, article.getRecommended());
        assertEquals(0, article.getRecommendSort());
    }

    private ArticleRequest baseRequest(String title, String summary, String content) {
        ArticleRequest request = new ArticleRequest();
        request.setTitle(title);
        request.setSummary(summary);
        request.setContent(content);
        request.setContentType("HTML");
        request.setStatus("PUBLISHED");
        return request;
    }
}
