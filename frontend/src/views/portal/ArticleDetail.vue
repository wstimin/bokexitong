<template>
  <div class="page">
    <PortalNav />
    <main class="shell detail">
      <article class="detail-article">
        <img v-if="article.coverUrl" class="cover detail-cover" :src="coverSrc" :alt="article.title" />
        <div class="meta detail-meta">
          <span>{{ article.categoryName || '未分类' }}</span>
          <span>{{ displayDate }}</span>
          <span>{{ article.authorName || '匿名作者' }}</span>
          <span>{{ article.viewCount || 0 }} 阅读</span>
          <span>{{ article.likeCount || 0 }} 点赞</span>
          <span>{{ article.favoriteCount || 0 }} 收藏</span>
        </div>

        <h1>{{ article.title }}</h1>
        <p v-if="article.summary" class="detail-summary">{{ article.summary }}</p>

        <div v-if="article.tags?.length" class="tag-row detail-tags">
          <span v-for="tag in article.tags" :key="tag.id" class="anime-tag" :style="{ color: tag.color }">
            {{ tag.name }}
          </span>
        </div>

        <div class="hero-actions detail-actions">
          <button class="btn-primary" @click="like">点赞</button>
          <button class="btn-ghost" @click="favorite">收藏</button>
        </div>

        <div class="article-body" v-html="html"></div>
      </article>

      <section class="detail-article comment-section">
        <div class="section-title">
          <h2>评论</h2>
          <span class="section-subtitle">评论通过审核后会公开显示</span>
        </div>
        <el-input v-model="commentText" type="textarea" :rows="3" placeholder="写下你的评论" />
        <button class="btn-primary comment-submit" :disabled="!commentText.trim()" @click="submitComment">提交评论</button>
        <div v-if="comments.length" class="comment-list">
          <div v-for="item in comments" :key="item.id" class="comment-item">
            <div class="meta">{{ formatDate(item.createdAt) }}</div>
            <p>{{ item.content }}</p>
          </div>
        </div>
        <el-empty v-else description="暂无公开评论" :image-size="72" />
      </section>
    </main>
    <PortalFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PortalNav from '../../components/PortalNav.vue'
import PortalFooter from '../../components/PortalFooter.vue'
import { articleApi, commentApi, portalApi } from '../../api/blog'
import { useAuthStore } from '../../stores/auth'
import { normalizeAssetUrl } from '../../utils/assets'
import { toDisplayHtml } from '../../utils/richText'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const article = ref({})
const comments = ref([])
const commentText = ref('')
const html = computed(() => toDisplayHtml(article.value.content, article.value.contentType))
const coverSrc = computed(() => normalizeAssetUrl(article.value.coverUrl))
const displayDate = computed(() => formatDate(article.value.publishedAt || article.value.createdAt).slice(0, 10) || '未发布')

const load = async () => {
  const [detail, commentPage] = await Promise.all([
    portalApi.detail(route.params.id),
    commentApi.page({ articleId: route.params.id })
  ])
  article.value = detail.data || {}
  comments.value = commentPage.data.records || []
}

const requireLogin = () => {
  if (auth.isUserLogin) return true
  router.push(`/login?redirect=/article/${route.params.id}`)
  return false
}

const like = async () => {
  if (!requireLogin()) return
  const res = await articleApi.like(route.params.id)
  article.value.likeCount = res.data.count
  ElMessage.success(res.data.active ? '已点赞' : '已取消点赞')
}

const favorite = async () => {
  if (!requireLogin()) return
  const res = await articleApi.favorite(route.params.id)
  article.value.favoriteCount = res.data.count
  ElMessage.success(res.data.active ? '已收藏' : '已取消收藏')
}

const submitComment = async () => {
  if (!requireLogin() || !commentText.value.trim()) return
  await commentApi.save({ articleId: route.params.id, content: commentText.value.trim() })
  commentText.value = ''
  ElMessage.success('评论已提交，审核通过后会显示')
}

const formatDate = (date) => String(date || '').slice(0, 16) || '-'

onMounted(load)
</script>
