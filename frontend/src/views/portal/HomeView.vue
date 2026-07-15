<template>
  <div class="page">
    <PortalNav />
    <section
      class="hero hero-compact"
      :class="{ 'hero-fallback': !heroBackgroundSrc, 'hero-synced': Boolean(site.backgroundSrc) }"
      :style="heroStyle"
    >
      <div class="shell hero-feature">
        <div class="hero-content">
          <span class="eyebrow">{{ site.heroBadge }}</span>
          <h1>{{ site.heroTitle }}</h1>
          <p>{{ site.heroSubtitle }}</p>
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
            <img v-if="recommendCoverSrc" :src="recommendCoverSrc" :alt="activeRecommended.title" />
            <div v-else class="recommend-placeholder">{{ activeRecommended.title?.slice(0, 1) || '文' }}</div>
            <div class="recommend-body">
              <div class="card-kicker">
                <span>{{ activeRecommended.categoryName || '未分类' }}</span>
                <span>{{ activeRecommended.publishedAt || activeRecommended.createdAt || '' }}</span>
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

    <main id="articles" class="shell main-grid">
      <section>
        <div class="section-title article-toolbar">
          <div>
            <h2>{{ activeCategoryName }}</h2>
            <p class="section-subtitle">{{ filterSummary }}，共 {{ articlePage.total }} 篇文章</p>
          </div>
          <div class="article-search">
            <el-input
              v-model="keyword"
              placeholder="搜索标题、摘要或正文"
              clearable
              @keyup.enter="searchArticles"
              @clear="searchArticles"
            />
            <button class="btn-ghost" type="button" :disabled="articleLoading" @click="searchArticles">搜索</button>
            <button v-if="hasActiveFilter" class="btn-ghost" type="button" :disabled="articleLoading" @click="resetFilters">重置</button>
          </div>
        </div>

        <div v-loading="articleLoading" class="article-grid article-results">
          <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
        </div>
        <el-empty v-if="!articleLoading && articles.length === 0" description="没有找到相关文章" />
        <div v-if="articlePage.total > articlePage.size" class="pager">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="articlePage.total"
            :page-size="articlePage.size"
            v-model:current-page="articlePage.current"
            @current-change="loadArticles"
          />
        </div>
      </section>

      <aside>
        <div class="side-panel">
          <h3>分类</h3>
          <el-input v-model="categoryKeyword" placeholder="搜索分类" clearable size="small" class="category-search" />
          <div class="category-list category-scroll">
            <button class="category-item category-button" type="button" :class="{ active: selectedCategoryId === null }" @click="selectCategory(null)">
              <span>全部文章</span><span>{{ articlePage.total }}</span>
            </button>
            <button
              v-for="item in filteredCategories"
              :key="item.id"
              class="category-item category-button"
              type="button"
              :class="{ active: selectedCategoryId === item.id }"
              @click="selectCategory(item.id)"
            >
              <span>{{ item.name }}</span><span>#</span>
            </button>
          </div>
          <el-empty v-if="filteredCategories.length === 0" description="暂无匹配分类" :image-size="72" />
        </div>
        <div class="side-panel">
          <h3>标签</h3>
          <div class="tag-row">
            <button
              v-for="tag in tags"
              :key="tag.id"
              class="anime-tag tag-button"
              type="button"
              :class="{ active: selectedTagId === tag.id }"
              :style="{ color: tag.color }"
              @click="selectTag(tag)"
            >
              {{ tag.name }}
            </button>
          </div>
        </div>
      </aside>
    </main>
    <PortalFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import PortalNav from '../../components/PortalNav.vue'
import PortalFooter from '../../components/PortalFooter.vue'
import ArticleCard from '../../components/ArticleCard.vue'
import { portalApi } from '../../api/blog'
import { useSiteStore } from '../../stores/site'
import { normalizeAssetUrl } from '../../utils/assets'

const articles = ref([])
const categories = ref([])
const tags = ref([])
const hero = ref([])
const recommendedArticles = ref([])
const activeRecommendIndex = ref(0)
const site = useSiteStore()
const keyword = ref('')
const categoryKeyword = ref('')
const selectedCategoryId = ref(null)
const selectedTagId = ref(null)
const articleLoading = ref(false)
const articlePage = reactive({ current: 1, size: 9, total: 0 })

const heroUrl = computed(() => normalizeAssetUrl(hero.value[0]?.url))
const heroBackgroundSrc = computed(() => site.backgroundSrc || heroUrl.value)
const heroStyle = computed(() => (!site.backgroundSrc && heroBackgroundSrc.value)
  ? { backgroundImage: `url("${heroBackgroundSrc.value}")` }
  : {})
const activeRecommended = computed(() => recommendedArticles.value[activeRecommendIndex.value] || {})
const recommendCoverSrc = computed(() => normalizeAssetUrl(activeRecommended.value.coverUrl))
const filteredCategories = computed(() => {
  const key = categoryKeyword.value.trim().toLowerCase()
  if (!key) return categories.value
  return categories.value.filter((item) => item.name?.toLowerCase().includes(key))
})
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
  const settings = res.data.settings || {}
  site.name = settings.siteName || site.name
  site.heroTitle = settings.heroTitle || site.name
  site.heroSubtitle = settings.heroSubtitle || site.heroSubtitle
  site.heroBadge = settings.heroBadge || site.heroBadge
  site.backgroundUrl = settings.backgroundUrl || ''
  site.contactHtml = settings.contactHtml || ''
  site.logoUrl = (Array.isArray(res.data.logo) ? res.data.logo[0] : res.data.logo)?.url || site.logoUrl
  site.loaded = true
  site.applyHead()
  categories.value = res.data.categories || []
  tags.value = res.data.tags || []
  hero.value = res.data.hero || []
  recommendedArticles.value = res.data.recommendedArticles || res.data.articles || []
  activeRecommendIndex.value = 0
}

const loadArticles = async () => {
  articleLoading.value = true
  try {
    const res = await portalApi.articles({
      current: articlePage.current,
      size: articlePage.size,
      keyword: keyword.value.trim() || undefined,
      categoryId: selectedCategoryId.value || undefined,
      tagId: selectedTagId.value || undefined
    })
    articles.value = res.data.records || []
    articlePage.total = res.data.total || 0
  } finally {
    articleLoading.value = false
  }
}

const searchArticles = () => {
  articlePage.current = 1
  loadArticles()
}

const selectCategory = (id) => {
  selectedCategoryId.value = id
  articlePage.current = 1
  loadArticles()
}

const selectTag = (tag) => {
  selectedTagId.value = selectedTagId.value === tag.id ? null : tag.id
  articlePage.current = 1
  loadArticles()
}

const resetFilters = () => {
  keyword.value = ''
  selectedCategoryId.value = null
  selectedTagId.value = null
  articlePage.current = 1
  loadArticles()
}

const selectRecommend = (index) => {
  activeRecommendIndex.value = index
}

const prevRecommend = () => {
  if (!recommendedArticles.value.length) return
  activeRecommendIndex.value = (activeRecommendIndex.value - 1 + recommendedArticles.value.length) % recommendedArticles.value.length
}

const nextRecommend = () => {
  if (!recommendedArticles.value.length) return
  activeRecommendIndex.value = (activeRecommendIndex.value + 1) % recommendedArticles.value.length
}

onMounted(async () => {
  try {
    await loadHomeMeta()
  } catch (error) {
    console.error(error)
  }
  await loadArticles()
})
</script>
