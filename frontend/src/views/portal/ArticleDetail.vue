<template>
  <div class="page">
    <PortalNav />
    <main class="shell detail">
      <div class="detail-layout">
      <article class="detail-article">
        <header class="article-header">
        <span class="section-kicker">{{ article.categoryName || 'UNCATEGORIZED' }}</span>
        <h1>{{ article.title }}</h1>
        <p v-if="article.summary" class="detail-summary">{{ article.summary }}</p>
        <div class="meta detail-meta">
          <span>{{ article.categoryName || '未分类' }}</span>
          <span>{{ displayDate }}</span>
          <span>{{ article.authorName || '匿名作者' }}</span>
          <span>{{ readingMinutes }} 分钟阅读</span>
          <span>{{ article.viewCount || 0 }} 阅读</span>
          <span>{{ article.likeCount || 0 }} 点赞</span>
          <span>{{ article.favoriteCount || 0 }} 收藏</span>
        </div>
        </header>

        <img v-if="article.coverUrl" class="cover detail-cover" :src="coverSrc" :alt="article.title" decoding="async" />

        <div v-if="article.tags?.length" class="tag-row detail-tags">
          <span v-for="tag in article.tags" :key="tag.id" class="anime-tag" :style="{ color: tag.color }">
            {{ tag.name }}
          </span>
        </div>

        <div class="hero-actions detail-actions">
          <button class="btn-primary" type="button" @click="like">点赞</button>
          <button class="btn-ghost" type="button" @click="favorite">收藏</button>
          <button class="btn-ghost" type="button" @click="shareArticle">分享文章</button>
        </div>

        <div ref="articleBodyRef" class="article-body" v-html="html"></div>
      </article>

      <aside v-if="toc.length" class="article-toc">
        <button class="toc-heading" type="button" :aria-expanded="tocOpen" @click="tocOpen = !tocOpen">
          <span>文章目录</span><span>{{ tocOpen ? '−' : '+' }}</span>
        </button>
        <nav v-show="tocOpen">
          <a v-for="item in toc" :key="item.id" :class="`toc-level-${item.level}`" :href="`#${item.id}`">{{ item.text }}</a>
        </nav>
      </aside>
      </div>

      <nav v-if="context.previous || context.next" class="post-navigation">
        <RouterLink v-if="context.previous" :to="`/article/${context.previous.id}`"><span>上一篇</span><strong>{{ context.previous.title }}</strong></RouterLink>
        <span v-else></span>
        <RouterLink v-if="context.next" :to="`/article/${context.next.id}`"><span>下一篇</span><strong>{{ context.next.title }}</strong></RouterLink>
      </nav>

      <section v-if="context.related?.length" class="related-section">
        <div class="section-title"><div><span class="section-kicker">KEEP READING</span><h2>相关文章</h2></div></div>
        <div class="article-grid related-grid"><ArticleCard v-for="item in context.related" :key="item.id" :article="item" /></div>
      </section>

      <section class="detail-article comment-section">
        <div class="section-title">
          <h2>评论</h2>
          <span class="section-subtitle">评论可直接发布，命中违禁词会自动驳回</span>
        </div>
        <el-input v-model="commentText" type="textarea" :rows="3" placeholder="写下你的评论" />
        <button class="btn-primary comment-submit" type="button" :disabled="!commentText.trim()" @click="submitComment">提交评论</button>
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
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PortalNav from '../../components/PortalNav.vue'
import PortalFooter from '../../components/PortalFooter.vue'
import ArticleCard from '../../components/ArticleCard.vue'
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
const context = ref({ previous: null, next: null, related: [] })
const articleBodyRef = ref()
const toc = ref([])
const tocOpen = ref(true)
const html = computed(() => toDisplayHtml(article.value.content, article.value.contentType))
const coverSrc = computed(() => normalizeAssetUrl(article.value.coverUrl))
const displayDate = computed(() => formatDate(article.value.publishedAt || article.value.createdAt).slice(0, 10) || '未发布')
const readingMinutes = computed(() => Math.max(1, Math.ceil(String(article.value.content || '').replace(/<[^>]+>/g, '').length / 420)))

const buildToc = async () => {
  await nextTick()
  const headings = [...(articleBodyRef.value?.querySelectorAll('h2, h3') || [])]
  toc.value = headings.map((heading, index) => {
    const id = `section-${index + 1}`
    heading.id = id
    return { id, text: heading.textContent?.trim() || `章节 ${index + 1}`, level: heading.tagName === 'H3' ? 3 : 2 }
  })
}

const load = async () => {
  const [detail, commentPage, readingContext] = await Promise.all([
    portalApi.detail(route.params.id),
    commentApi.page({ articleId: route.params.id }),
    portalApi.articleContext(route.params.id)
  ])
  article.value = detail.data || {}
  comments.value = commentPage.data.records || []
  context.value = readingContext.data || { previous: null, next: null, related: [] }
  await buildToc()
}

const requireLogin = () => {
  if (auth.isUserLogin) return true
  router.push(`/login?redirect=/article/${route.params.id}`)
  return false
}

const like = async () => {
  if (!requireLogin()) return
  try {
    const res = await articleApi.like(route.params.id)
    article.value.likeCount = res.data.count
    ElMessage.success(res.data.active ? '已点赞' : '已取消点赞')
  } catch (error) {
    console.error(error)
  }
}

const favorite = async () => {
  if (!requireLogin()) return
  try {
    const res = await articleApi.favorite(route.params.id)
    article.value.favoriteCount = res.data.count
    ElMessage.success(res.data.active ? '已收藏' : '已取消收藏')
  } catch (error) {
    console.error(error)
  }
}

const shareArticle = async () => {
  const payload = { title: article.value.title, text: article.value.summary || article.value.title, url: window.location.href }
  try {
    if (navigator.share) await navigator.share(payload)
    else {
      await navigator.clipboard.writeText(window.location.href)
      ElMessage.success('文章链接已复制')
    }
  } catch (error) {
    if (error?.name !== 'AbortError') console.error(error)
  }
}

const submitComment = async () => {
  if (!requireLogin() || !commentText.value.trim()) return
  try {
    const res = await commentApi.save({ articleId: route.params.id, content: commentText.value.trim() })
    commentText.value = ''
    if (res.data?.status === 'APPROVED') {
      comments.value = [res.data, ...comments.value]
      ElMessage.success('评论已提交并直接显示')
    } else {
      ElMessage.warning(res.data?.reviewReason || '评论因违禁词被驳回')
    }
  } catch (error) {
    console.error(error)
  }
}

const formatDate = (date) => String(date || '').slice(0, 16) || '-'

onMounted(() => {
  load().catch((error) => console.error(error))
})

watch(() => route.params.id, () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
  load().catch((error) => console.error(error))
})
</script>
