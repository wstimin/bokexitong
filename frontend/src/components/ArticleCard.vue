<template>
  <RouterLink class="article-card" :to="`/article/${article.id}`">
    <img v-if="article.coverUrl" class="cover" :src="coverSrc" :alt="article.title" />
    <div v-else class="cover cover-placeholder">{{ article.categoryName || '未设封面' }}</div>

    <div class="card-body">
      <div class="card-kicker">
        <span>{{ article.categoryName || '未分类' }}</span>
        <span>{{ displayDate }}</span>
      </div>

      <h3>{{ article.title }}</h3>
      <p>{{ article.summary || '阅读全文查看完整内容。' }}</p>

      <div class="meta card-stats">
        <span>{{ article.authorName || '匿名作者' }}</span>
        <span>{{ article.viewCount || 0 }} 阅读</span>
        <span>{{ article.likeCount || 0 }} 点赞</span>
        <span>{{ article.favoriteCount || 0 }} 收藏</span>
      </div>

      <div v-if="article.tags?.length" class="tag-row">
        <span v-for="tag in article.tags" :key="tag.id" class="anime-tag" :style="{ color: tag.color }">
          {{ tag.name }}
        </span>
      </div>
    </div>
  </RouterLink>
</template>

<script setup>
import { computed } from 'vue'
import { normalizeAssetUrl } from '../utils/assets'

const props = defineProps({ article: { type: Object, required: true } })

const coverSrc = computed(() => normalizeAssetUrl(props.article.coverUrl))
const displayDate = computed(() => {
  const raw = props.article.publishedAt || props.article.createdAt
  if (!raw) return '未发布'
  return String(raw).slice(0, 10)
})
</script>
