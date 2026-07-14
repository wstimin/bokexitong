package com.example.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.entity.Article;
import com.example.blog.entity.Favorite;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface FavoriteMapper extends BaseMapper<Favorite> {
    @Select("""
            SELECT a.*
            FROM favorite f
            INNER JOIN article a ON a.id = f.article_id
            WHERE f.user_id = #{userId}
              AND a.status = 'PUBLISHED'
              AND (a.deleted IS NULL OR a.deleted = 0)
            ORDER BY f.created_at DESC
            """)
    Page<Article> selectFavoriteArticles(Page<Article> page, @Param("userId") Long userId);
}
