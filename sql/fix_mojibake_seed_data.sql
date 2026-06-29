USE personal_blog;

UPDATE blog_user
SET nickname = '站长'
WHERE username = 'admin';

UPDATE blog_user
SET nickname = '演示用户'
WHERE username = 'demo';

UPDATE category
SET name = '日常随笔', description = '生活碎片与灵感记录', sort = 1
WHERE id = 1;

UPDATE category
SET name = '技术笔记', description = 'Java、Vue、数据库与工具链', sort = 2
WHERE id = 2;

UPDATE category
SET name = '动漫杂谈', description = '番剧感想与角色分析', sort = 3
WHERE id = 3;

UPDATE tag
SET name = 'SpringBoot3', color = '#60a5fa'
WHERE id = 1;

UPDATE tag
SET name = 'Vue3', color = '#3ad9c9'
WHERE id = 2;

UPDATE tag
SET name = 'Markdown', color = '#ff77b7'
WHERE id = 3;

UPDATE tag
SET name = '二次元', color = '#a78bfa'
WHERE id = 4;

UPDATE article
SET title = '用 Spring Boot 3 和 Vue 3 搭一个二次元博客',
    summary = '从接口、权限、文章创作到后台仪表盘，记录一个前后端分离博客系统的搭建过程。',
    content = '# 开篇\n\n这是一个支持 Markdown、代码高亮、评论互动和后台管理的个人博客系统。\n\n```java\nSystem.out.println("Hello Blog");\n```'
WHERE id = 1;

UPDATE article
SET title = '给博客加一点番剧感 UI',
    summary = '用柔和色彩、徽章标签、横幅图和卡片动效，让内容系统更有个人气质。',
    content = '## 设计思路\n\n二次元风格不等于堆满元素，关键是色彩、节奏和细节统一。'
WHERE id = 2;

UPDATE image_resource
SET title = '首页横幅占位图',
    description = '后台可替换为你自己的二次元横幅 URL'
WHERE id = 1;
