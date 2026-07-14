USE personal_blog;

UPDATE site_setting
SET setting_value = '博客系统'
WHERE setting_key = 'siteName'
  AND setting_value REGEXP 'å|ã|Ã|Â|ç|¢|®|»|¿|�|鍗氬|绯荤粺|杩欐|涓€|銆|鈥';

UPDATE site_setting
SET setting_value = '博客系统'
WHERE setting_key = 'heroTitle'
  AND setting_value REGEXP 'å|ã|Ã|Â|ç|¢|®|»|¿|�|鍗氬|绯荤粺|杩欐|涓€|銆|鈥';

UPDATE site_setting
SET setting_value = '博客'
WHERE setting_key = 'heroBadge'
  AND setting_value REGEXP 'å|ã|Ã|Â|ç|¢|®|»|¿|�|鍗氬|绯荤粺|杩欐|涓€|銆|鈥';

UPDATE site_setting
SET setting_value = '用卡片浏览公开文章，点开后再阅读完整内容。登录后可以进入用户中心创作、管理自己的文章。'
WHERE setting_key = 'heroSubtitle'
  AND setting_value REGEXP 'å|ã|Ã|Â|ç|¢|®|»|¿|�|鍗氬|绯荤粺|杩欐|涓€|銆|鈥';

UPDATE site_setting
SET setting_value = '博客系统'
WHERE setting_key = 'mailFromName'
  AND setting_value REGEXP 'å|ã|Ã|Â|ç|¢|®|»|¿|�|鍗氬|绯荤粺|杩欐|涓€|銆|鈥';
