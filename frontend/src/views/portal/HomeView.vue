<template>
  <div class="page">
    <PortalNav />

    <section class="hero">
      <div class="shell hero-feature">
        <div class="hero-content" :style="heroTextStyle">
          <span class="eyebrow">{{ site.heroBadge }} · CURATED JOURNAL</span>
          <h1>{{ site.heroTitle }}</h1>
          <p>{{ site.heroSubtitle }}</p>
          <div class="hero-actions hero-cta">
            <a class="btn-primary" href="#articles">开始阅读</a>
            <RouterLink class="btn-ghost hero-secondary" to="/archive">浏览全部文章</RouterLink>
          </div>
        </div>

        <aside v-if="recommendedArticles.length" class="recommend-panel">
          <div class="recommend-head">
            <div>
              <span class="recommend-kicker">推荐阅读</span>
              <strong>{{ activeRecommendIndex + 1 }} / {{ recommendedArticles.length }}</strong>
            </div>
            <div class="recommend-nav">
              <button type="button" aria-label="上一篇推荐" @click="prevRecommend">‹</button>
              <button type="button" aria-label="下一篇推荐" @click="nextRecommend">›</button>
            </div>
          </div>

          <RouterLink class="recommend-card" :to="`/article/${activeRecommended.id}`">
            <img v-if="recommendCoverSrc" :src="recommendCoverSrc" :alt="activeRecommended.title" decoding="async" />
            <div v-else class="recommend-placeholder">{{ activeRecommended.title?.slice(0, 1) || '文' }}</div>
            <div class="recommend-body">
              <div class="card-kicker">
                <span>{{ activeRecommended.categoryName || '未分类' }}</span>
                <span>{{ formatDate(activeRecommended.publishedAt || activeRecommended.createdAt) }}</span>
              </div>
              <h2>{{ activeRecommended.title }}</h2>
              <p>{{ activeRecommended.summary || '这篇文章暂未填写摘要。' }}</p>
              <div class="meta card-stats">
                <span>{{ activeRecommended.viewCount || 0 }} 阅读</span>
                <span>{{ activeRecommended.likeCount || 0 }} 点赞</span>
                <span>{{ activeRecommended.favoriteCount || 0 }} 收藏</span>
              </div>
            </div>
          </RouterLink>

          <div class="recommend-track">
            <button
              v-for="(item, index) in recommendedArticles"
              :key="item.id"
              type="button"
              :class="['recommend-chip', { active: activeRecommendIndex === index }]"
              @click="selectRecommend(index)"
            >
              {{ item.title }}
            </button>
          </div>
        </aside>
      </div>
    </section>

    <section class="editorial-strip">
      <div class="shell editorial-strip__inner">
        <div><strong>{{ articlePage.total }}</strong><span>篇公开文章</span></div>
        <div><strong>{{ categories.length }}</strong><span>个内容分类</span></div>
        <div><strong>{{ tags.length }}</strong><span>个灵感标签</span></div>
        <p>慢下来阅读，认真地记录。<br />每一篇内容都值得被好好呈现。</p>
      </div>
    </section>

    <main id="articles" class="shell home-articles">
      <section class="article-main">
        <div class="section-title article-toolbar">
          <div>
            <span class="section-kicker">LATEST STORIES</span>
            <h2>{{ activeCategoryName }}</h2>
            <p class="section-subtitle">{{ filterSummary }}，共 {{ articlePage.total }} 篇文章</p>
          </div>
        </div>

        <div class="article-filterbar" role="search" aria-label="文章筛选">
          <div class="article-search">
            <el-input v-model="keyword" placeholder="搜索标题、摘要或正文" clearable @keyup.enter="searchArticles" @clear="searchArticles" />
            <button class="btn-ghost filter-action" type="button" @click="searchArticles">搜索</button>
          </div>

          <label class="filter-select">
            <span>分类</span>
            <el-select v-model="selectedCategoryId" placeholder="全部文章" clearable @change="selectCategory">
              <el-option label="全部文章" :value="null" />
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </label>

          <label class="filter-select">
            <span>标签</span>
            <el-select v-model="selectedTagId" placeholder="全部标签" clearable @change="selectTag">
              <el-option label="全部标签" :value="null" />
              <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
            </el-select>
          </label>

          <button v-if="hasActiveFilter" class="filter-reset" type="button" @click="resetFilters">清除筛选</button>
          <span class="filter-status" :class="{ active: articleLoading }" aria-live="polite">
            {{ articleLoading ? '正在更新…' : '' }}
          </span>
        </div>

        <div class="article-results" :class="{ 'is-updating': articleLoading }" :aria-busy="articleLoading">
          <div v-if="articleLoading && articles.length === 0" class="article-grid skeleton-grid" aria-hidden="true">
            <div v-for="index in 4" :key="index" class="article-skeleton">
              <span class="skeleton-cover"></span>
              <span class="skeleton-line skeleton-line--short"></span>
              <span class="skeleton-line"></span>
              <span class="skeleton-line"></span>
            </div>
          </div>
          <div v-else class="article-grid">
          <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
          </div>
        </div>
        <el-empty v-if="!articleLoading && articles.length === 0" description="没有找到相关文章" />
        <div v-if="articlePage.total > articlePage.size" class="pager">
          <el-pagination v-model:current-page="articlePage.current" background layout="prev, pager, next" :total="articlePage.total" :page-size="articlePage.size" @current-change="loadArticles" />
        </div>
      </section>
    </main>

    <PortalFooter />
  </div>
</template>

<script setup>
import axios from 'axios'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import PortalNav from '../../components/PortalNav.vue'
import PortalFooter from '../../components/PortalFooter.vue'
import ArticleCard from '../../components/ArticleCard.vue'
import { portalApi } from '../../api/blog'
import { useSiteStore } from '../../stores/site'
import { normalizeAssetUrl } from '../../utils/assets'
import { createHeroTextStyle } from '../../utils/heroAppearance'

const articles = ref([])
const categories = ref([])
const tags = ref([])
const hero = ref([])
const recommendedArticles = ref([])
const activeRecommendIndex = ref(0)
const site = useSiteStore()
const keyword = ref('')
const selectedCategoryId = ref(null)
const selectedTagId = ref(null)
const articleLoading = ref(false)
const articlePage = reactive({ current: 1, size: 9, total: 0 })

const heroTextStyle = computed(() => createHeroTextStyle(site.heroTitleColor, site.heroSubtitleColor, site.heroTextShadow))
const activeRecommended = computed(() => recommendedArticles.value[activeRecommendIndex.value] || {})
const recommendCoverSrc = computed(() => normalizeAssetUrl(activeRecommended.value.coverUrl))
const activeCategoryName = computed(() => {
  if (!selectedCategoryId.value) return '最新文章'
  return categories.value.find((item) => item.id === selectedCategoryId.value)?.name || '分类文章'
})
const activeTagName = computed(() => tags.value.find((item) => item.id === selectedTagId.value)?.name || '')
const hasActiveFilter = computed(() => Boolean(keyword.value.trim() || selectedCategoryId.value || selectedTagId.value))
const filterSummary = computed(() => {
  const filters = []
  if (selectedCategoryId.value) filters.push(`分类：${activeCategoryName.value}`)
  if (selectedTagId.value) filters.push(`标签：${activeTagName.value}`)
  if (keyword.value.trim()) filters.push(`关键词：${keyword.value.trim()}`)
  return filters.length ? filters.join(' / ') : '全部公开文章'
})

const loadHomeMeta = async () => {
  const res = await portalApi.home()
  const data = res.data || {}
  categories.value = data.categories || []
  tags.value = data.tags || []
  hero.value = data.hero || []
  recommendedArticles.value = data.recommendedArticles || data.articles || []
  activeRecommendIndex.value = 0
}

let articleRequestController = null
let articleRequestSequence = 0
let filterTimer = null

const loadArticles = async () => {
  window.clearTimeout(filterTimer)
  filterTimer = null
  articleRequestController?.abort()
  articleRequestController = new AbortController()
  const requestSequence = ++articleRequestSequence
  articleLoading.value = true
  try {
    const res = await portalApi.articles({
      current: articlePage.current,
      size: articlePage.size,
      keyword: keyword.value.trim() || undefined,
      categoryId: selectedCategoryId.value || undefined,
      tagId: selectedTagId.value || undefined
    }, { signal: articleRequestController.signal })
    if (requestSequence !== articleRequestSequence) return
    articles.value = res.data.records || []
    articlePage.total = res.data.total || 0
  } catch (error) {
    if (!axios.isCancel(error) && error.code !== 'ERR_CANCELED') throw error
  } finally {
    if (requestSequence === articleRequestSequence) {
      articleLoading.value = false
      articleRequestController = null
    }
  }
}

const scheduleArticleLoad = () => {
  window.clearTimeout(filterTimer)
  articleRequestSequence += 1
  articleRequestController?.abort()
  articleRequestController = null
  articleLoading.value = true
  filterTimer = window.setTimeout(loadArticles, 140)
}

const searchArticles = () => {
  articlePage.current = 1
  loadArticles()
}

const selectCategory = (id) => {
  selectedCategoryId.value = id
  articlePage.current = 1
  scheduleArticleLoad()
}

const selectTag = (id) => {
  selectedTagId.value = id
  articlePage.current = 1
  scheduleArticleLoad()
}

const resetFilters = () => {
  keyword.value = ''
  selectedCategoryId.value = null
  selectedTagId.value = null
  articlePage.current = 1
  loadArticles()
}

const selectRecommend = (index) => { activeRecommendIndex.value = index }
const prevRecommend = () => {
  if (!recommendedArticles.value.length) return
  activeRecommendIndex.value = (activeRecommendIndex.value - 1 + recommendedArticles.value.length) % recommendedArticles.value.length
}
const nextRecommend = () => {
  if (!recommendedArticles.value.length) return
  activeRecommendIndex.value = (activeRecommendIndex.value + 1) % recommendedArticles.value.length
}
const formatDate = (date) => String(date || '').slice(0, 10)

onMounted(async () => {
  const results = await Promise.allSettled([loadHomeMeta(), loadArticles()])
  results
    .filter((result) => result.status === 'rejected')
    .forEach((result) => console.error(result.reason))
})

onBeforeUnmount(() => {
  window.clearTimeout(filterTimer)
  articleRequestController?.abort()
})
</script>
