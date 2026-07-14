package com.example.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.dto.ArticleCardResponse;
import com.example.blog.dto.ArticleDetailResponse;
import com.example.blog.dto.ArticleRequest;
import com.example.blog.entity.Article;
import com.example.blog.entity.ArticleTag;
import com.example.blog.entity.BlogUser;
import com.example.blog.entity.Category;
import com.example.blog.entity.Comment;
import com.example.blog.entity.Favorite;
import com.example.blog.entity.LikeRecord;
import com.example.blog.entity.Tag;
import com.example.blog.mapper.ArticleMapper;
import com.example.blog.mapper.ArticleTagMapper;
import com.example.blog.mapper.BlogUserMapper;
import com.example.blog.mapper.CategoryMapper;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.mapper.FavoriteMapper;
import com.example.blog.mapper.LikeRecordMapper;
import com.example.blog.mapper.TagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ArticleService {
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CommentMapper commentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteMapper favoriteMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final BlogUserMapper blogUserMapper;

    public ArticleService(ArticleMapper articleMapper, ArticleTagMapper articleTagMapper, CommentMapper commentMapper,
                          LikeRecordMapper likeRecordMapper, FavoriteMapper favoriteMapper,
                          CategoryMapper categoryMapper, TagMapper tagMapper, BlogUserMapper blogUserMapper) {
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.commentMapper = commentMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteMapper = favoriteMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.blogUserMapper = blogUserMapper;
    }

    public Page<Article> page(long current, long size, String keyword, String status, Long categoryId, Long tagId) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        final List<Long> taggedArticleIds;
        if (tagId != null) {
            taggedArticleIds = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                            .eq(ArticleTag::getTagId, tagId))
                    .stream()
                    .map(ArticleTag::getArticleId)
                    .toList();
            if (taggedArticleIds.isEmpty()) {
                Page<Article> emptyPage = new Page<>(current, size);
                emptyPage.setRecords(Collections.emptyList());
                emptyPage.setTotal(0);
                return emptyPage;
            }
        } else {
            taggedArticleIds = null;
        }
        return articleMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Article>()
                .and(hasKeyword, wrapper -> wrapper.like(Article::getTitle, keyword)
                        .or().like(Article::getSummary, keyword)
                        .or().like(Article::getContent, keyword))
                .eq(status != null && !status.isBlank(), Article::getStatus, status)
                .eq(categoryId != null, Article::getCategoryId, categoryId)
                .in(taggedArticleIds != null, Article::getId, taggedArticleIds)
                .orderByDesc(Article::getPublishedAt, Article::getCreatedAt));
    }

    public Page<ArticleCardResponse> publicCardPage(long current, long size, String keyword, Long categoryId, Long tagId) {
        Page<Article> articlePage = page(current, size, keyword, "PUBLISHED", categoryId, tagId);
        Page<ArticleCardResponse> responsePage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        responsePage.setRecords(toCardResponses(articlePage.getRecords()));
        return responsePage;
    }

    public Article detail(Long id, boolean increaseView) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new IllegalArgumentException("Article does not exist");
        }
        if (increaseView) {
            article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
            articleMapper.updateById(article);
        }
        return article;
    }

    public Article publicDetail(Long id) {
        Article article = detail(id, false);
        if (!"PUBLISHED".equals(article.getStatus())) {
            throw new IllegalArgumentException("Article does not exist");
        }
        article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
        articleMapper.updateById(article);
        return article;
    }

    public ArticleDetailResponse publicDetailResponse(Long id) {
        return toDetailResponse(publicDetail(id));
    }

    @Transactional
    public Article saveArticle(Long id, Long userId, ArticleRequest request) {
        Article article = id == null ? new Article() : detail(id, false);
        if (id != null && !userId.equals(article.getUserId())) {
            throw new IllegalArgumentException("You can only edit your own articles");
        }
        article.setUserId(article.getUserId() == null ? userId : article.getUserId());
        article.setCategoryId(request.getCategoryId());
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setCoverUrl(request.getCoverUrl());
        article.setContent(request.getContent());
        article.setContentType(request.getContentType() == null ? "MARKDOWN" : request.getContentType());
        article.setStatus(normalizeUserStatus(request.getStatus()));
        article.setUpdatedAt(LocalDateTime.now());
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

    public Article updateStatusByAdmin(Long id, String status) {
        Article article = detail(id, false);
        String nextStatus = normalizeAdminStatus(status);
        article.setStatus(nextStatus);
        article.setUpdatedAt(LocalDateTime.now());
        if ("PUBLISHED".equals(nextStatus) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        articleMapper.updateById(article);
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

    @Transactional
    public void removeByOwner(Long id, Long userId) {
        Article article = detail(id, false);
        if (!userId.equals(article.getUserId())) {
            throw new IllegalArgumentException("You can only delete your own articles");
        }
        removeBatch(List.of(id));
    }

    public Page<Article> userPage(Long userId, long current, long size, String keyword, String status) {
        return articleMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Article>()
                .eq(Article::getUserId, userId)
                .like(keyword != null && !keyword.isBlank(), Article::getTitle, keyword)
                .eq(status != null && !status.isBlank(), Article::getStatus, status)
                .orderByDesc(Article::getUpdatedAt, Article::getCreatedAt));
    }

    public Page<ArticleDetailResponse> userDetailPage(Long userId, long current, long size, String keyword, String status) {
        Page<Article> articlePage = userPage(userId, current, size, keyword, status);
        Page<ArticleDetailResponse> responsePage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        responsePage.setRecords(articlePage.getRecords().stream().map(this::toDetailResponse).toList());
        return responsePage;
    }

    public Page<ArticleCardResponse> favoritePage(Long userId, long current, long size) {
        Page<Article> articlePage = favoriteMapper.selectFavoriteArticles(new Page<>(current, size), userId);
        Page<ArticleCardResponse> responsePage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        responsePage.setRecords(toCardResponses(articlePage.getRecords()));
        return responsePage;
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

    private String normalizeUserStatus(String status) {
        if (status == null || status.isBlank() || "DRAFT".equals(status)) {
            return "DRAFT";
        }
        if ("PUBLISHED".equals(status) || "PENDING".equals(status)) {
            return "PENDING";
        }
        throw new IllegalArgumentException("Unsupported article status");
    }

    private String normalizeAdminStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        if (List.of("DRAFT", "PENDING", "PUBLISHED", "REJECTED", "OFFLINE").contains(status)) {
            return status;
        }
        throw new IllegalArgumentException("Unsupported article status");
    }

    private List<ArticleCardResponse> toCardResponses(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> categoryIds = articles.stream()
                .map(Article::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        List<Long> userIds = articles.stream()
                .map(Article::getUserId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        List<Long> articleIds = articles.stream().map(Article::getId).toList();

        Map<Long, Category> categories = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<Long, BlogUser> users = userIds.isEmpty()
                ? Collections.emptyMap()
                : blogUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(BlogUser::getId, Function.identity()));
        Map<Long, List<Tag>> tagsByArticle = loadTagsByArticle(articleIds);

        return articles.stream()
                .map(article -> toCardResponse(article, categories, users, tagsByArticle))
                .toList();
    }

    private ArticleCardResponse toCardResponse(Article article, Map<Long, Category> categories,
                                               Map<Long, BlogUser> users, Map<Long, List<Tag>> tagsByArticle) {
        ArticleCardResponse response = new ArticleCardResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setSummary(article.getSummary());
        response.setCoverUrl(article.getCoverUrl());
        response.setCategoryId(article.getCategoryId());
        response.setAuthorId(article.getUserId());
        response.setViewCount(article.getViewCount());
        response.setLikeCount(article.getLikeCount());
        response.setFavoriteCount(article.getFavoriteCount());
        response.setStatus(article.getStatus());
        response.setPublishedAt(article.getPublishedAt());
        response.setCreatedAt(article.getCreatedAt());
        response.setUpdatedAt(article.getUpdatedAt());
        Category category = categories.get(article.getCategoryId());
        BlogUser user = users.get(article.getUserId());
        response.setCategoryName(category == null ? null : category.getName());
        response.setAuthorName(user == null ? null : user.getNickname());
        response.setTags(tagsByArticle.getOrDefault(article.getId(), Collections.emptyList()));
        return response;
    }

    private ArticleDetailResponse toDetailResponse(Article article) {
        ArticleCardResponse card = toCardResponses(List.of(article)).get(0);
        ArticleDetailResponse response = new ArticleDetailResponse();
        response.setId(card.getId());
        response.setTitle(card.getTitle());
        response.setSummary(card.getSummary());
        response.setCoverUrl(card.getCoverUrl());
        response.setCategoryId(card.getCategoryId());
        response.setCategoryName(card.getCategoryName());
        response.setAuthorId(card.getAuthorId());
        response.setAuthorName(card.getAuthorName());
        response.setViewCount(card.getViewCount());
        response.setLikeCount(card.getLikeCount());
        response.setFavoriteCount(card.getFavoriteCount());
        response.setPublishedAt(card.getPublishedAt());
        response.setCreatedAt(card.getCreatedAt());
        response.setTags(card.getTags());
        response.setContent(article.getContent());
        response.setContentType(article.getContentType());
        return response;
    }

    private Map<Long, List<Tag>> loadTagsByArticle(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ArticleTag> relations = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getArticleId, articleIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = relations.stream()
                .map(ArticleTag::getTagId)
                .distinct()
                .toList();
        Map<Long, Tag> tags = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity()));
        return relations.stream()
                .filter(relation -> tags.containsKey(relation.getTagId()))
                .collect(Collectors.groupingBy(
                        ArticleTag::getArticleId,
                        Collectors.mapping(relation -> tags.get(relation.getTagId()), Collectors.toList())
                ));
    }
}
