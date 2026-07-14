USE personal_blog;

ALTER TABLE blog_user
  MODIFY email VARCHAR(120) NULL;

UPDATE blog_user
SET email = NULL
WHERE email = ''
   OR email = 'admin@example.com'
   OR email = CONCAT(username, '@example.com');

DROP PROCEDURE IF EXISTS add_blog_user_email_unique_index;
DELIMITER //
CREATE PROCEDURE add_blog_user_email_unique_index()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM (
      SELECT email
      FROM blog_user
      WHERE email IS NOT NULL AND email <> ''
      GROUP BY email
      HAVING COUNT(*) > 1
    ) duplicated_emails
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate user emails found. Please clean duplicated blog_user.email values before upgrading.';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_user'
      AND column_name = 'email'
      AND non_unique = 0
  ) THEN
    CREATE UNIQUE INDEX uk_blog_user_email ON blog_user(email);
  END IF;
END//
DELIMITER ;

CALL add_blog_user_email_unique_index();

DROP PROCEDURE IF EXISTS add_blog_user_email_unique_index;
