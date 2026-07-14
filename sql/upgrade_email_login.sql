USE personal_blog;

UPDATE blog_user
SET email = CONCAT(username, '@example.com')
WHERE email IS NULL OR email = '';

ALTER TABLE blog_user
  MODIFY email VARCHAR(120) NOT NULL;

CREATE UNIQUE INDEX uk_blog_user_email ON blog_user(email);
