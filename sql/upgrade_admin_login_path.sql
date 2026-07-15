USE personal_blog;

INSERT INTO site_setting(setting_key, setting_value, description)
SELECT 'adminLoginPath', '/admin/login', '后台登录路径'
WHERE NOT EXISTS (
  SELECT 1 FROM site_setting WHERE setting_key = 'adminLoginPath'
);
