USE personal_blog;

UPDATE blog_user
SET nickname = '站长'
WHERE username = 'admin'
  AND role = 'ADMIN'
  AND (
    nickname IS NULL
    OR nickname = ''
    OR nickname REGEXP 'å|ã|Ã|Â|ç|¢|®|»|¿|�|鍗氬|绯荤粺|ç«|é|æ|¤'
  );
