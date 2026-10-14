package com.example.blog.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseIndexUpgrade implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseIndexUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Idempotent, best-effort upgrades for existing installations. A fresh
        // installation already receives these indexes from the schema script.
        List<IndexDefinition> indexes = List.of(
                new IndexDefinition("article", "idx_article_public_list", "status, published_at, created_at"),
                new IndexDefinition("article", "idx_article_category_list", "status, category_id, published_at"),
                new IndexDefinition("article_tag", "idx_article_tag_reverse", "tag_id, article_id"),
                new IndexDefinition("comment", "idx_comment_public_list", "article_id, status, created_at"),
                new IndexDefinition("comment", "idx_comment_user_list", "user_id, created_at")
        );
        for (IndexDefinition index : indexes) {
            createIfMissing(index);
        }
    }

    private void createIfMissing(IndexDefinition index) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics " +
                            "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                    Integer.class,
                    index.table(),
                    index.name()
            );
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE `" + index.table() + "` ADD INDEX `" + index.name() + "` (" + index.columns() + ")");
            }
        } catch (Exception ignored) {
            // Installation mode may start before the target database exists.
            // The next normal startup retries, while the web installer creates
            // the same indexes in its schema transaction.
        }
    }

    private record IndexDefinition(String table, String name, String columns) {}
}
