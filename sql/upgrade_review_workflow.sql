USE personal_blog;

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(
  IN tableName VARCHAR(64),
  IN columnName VARCHAR(64),
  IN columnDefinition VARCHAR(500),
  IN afterColumn VARCHAR(64)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = tableName
      AND column_name = columnName
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `', columnName, '` ', columnDefinition, ' AFTER `', afterColumn, '`');
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL add_column_if_missing('article', 'review_reason', 'VARCHAR(500) NULL', 'published_at');
CALL add_column_if_missing('article', 'reviewed_at', 'DATETIME NULL', 'review_reason');
CALL add_column_if_missing('comment', 'review_reason', 'VARCHAR(500) NULL', 'status');
CALL add_column_if_missing('comment', 'reviewed_at', 'DATETIME NULL', 'review_reason');

DROP PROCEDURE IF EXISTS add_column_if_missing;
