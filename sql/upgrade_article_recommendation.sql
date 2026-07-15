USE personal_blog;

DROP PROCEDURE IF EXISTS add_article_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_article_column_if_missing(
  IN columnName VARCHAR(64),
  IN columnDefinition VARCHAR(500),
  IN afterColumn VARCHAR(64)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'article'
      AND column_name = columnName
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `article` ADD COLUMN `', columnName, '` ', columnDefinition, ' AFTER `', afterColumn, '`');
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//

CREATE PROCEDURE add_article_index_if_missing(
  IN indexName VARCHAR(64),
  IN indexDefinition VARCHAR(500)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'article'
      AND index_name = indexName
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `article` ADD INDEX `', indexName, '` ', indexDefinition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL add_article_column_if_missing('recommended', 'TINYINT DEFAULT 0', 'status');
CALL add_article_column_if_missing('recommend_sort', 'INT DEFAULT 0', 'recommended');
CALL add_article_index_if_missing('idx_article_recommend', '(`recommended`, `recommend_sort`)');

UPDATE article SET recommended = 0 WHERE recommended IS NULL;
UPDATE article SET recommend_sort = 0 WHERE recommend_sort IS NULL;

DROP PROCEDURE IF EXISTS add_article_column_if_missing;
DROP PROCEDURE IF EXISTS add_article_index_if_missing;
