USE personal_blog;

DROP PROCEDURE IF EXISTS add_blog_index_if_missing;
DELIMITER //
CREATE PROCEDURE add_blog_index_if_missing(
  IN tableName VARCHAR(64),
  IN indexName VARCHAR(64),
  IN indexDefinition VARCHAR(500)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = tableName
      AND index_name = indexName
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', tableName, '` ADD INDEX `', indexName, '` ', indexDefinition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL add_blog_index_if_missing('article', 'idx_article_public_list', '(`status`, `published_at`, `created_at`)');
CALL add_blog_index_if_missing('article', 'idx_article_category_list', '(`status`, `category_id`, `published_at`)');
CALL add_blog_index_if_missing('article_tag', 'idx_article_tag_reverse', '(`tag_id`, `article_id`)');
CALL add_blog_index_if_missing('comment', 'idx_comment_public_list', '(`article_id`, `status`, `created_at`)');
CALL add_blog_index_if_missing('comment', 'idx_comment_user_list', '(`user_id`, `created_at`)');

DROP PROCEDURE IF EXISTS add_blog_index_if_missing;
