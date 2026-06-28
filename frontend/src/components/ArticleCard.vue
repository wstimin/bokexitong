<template>
  <RouterLink class="article-card" :to="`/article/${article.id}`">
    <img v-if="article.coverUrl" class="cover" :src="article.coverUrl" :alt="article.title" />
    <div v-else class="cover"></div>
    <div class="card-body">
      <div class="meta">
        <span>{{ article.publishedAt || article.createdAt || '未发布' }}</span>
        <span>{{ article.viewCount || 0 }} 浏览</span>
      </div>
      <h3>{{ article.title }}</h3>
      <p>{{ article.summary || '这个作者很神秘，还没有写摘要。' }}</p>
      <div class="tag-row">
        <span class="anime-tag">Markdown</span>
        <span class="anime-tag">创作日记</span>
      </div>
    </div>
  </RouterLink>
</template>

<script setup>
defineProps({ article: { type: Object, required: true } })
</script>
