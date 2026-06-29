<template>
  <div class="page">
    <PortalNav />
    <section class="hero" :class="{ 'hero-fallback': !heroUrl }" :style="heroStyle">
      <div class="shell hero-content">
        <span class="eyebrow">Anime Style Personal Blog</span>
        <h1>星绘 Blog</h1>
        <p>这里展示最新发布的文章。登录后可以进入用户中心创作内容、维护资料和修改密码。</p>
      </div>
    </section>

    <main class="shell main-grid">
      <section>
        <div class="section-title article-toolbar">
          <div>
            <h2>{{ activeCategoryName }}</h2>
            <p class="section-subtitle">{{ filterSummary }}，共 {{ articlePage.total }} 篇文章</p>
          </div>
          <div class="article-search">
            <el-input
              v-model="keyword"
              placeholder="搜索文章标题"
              clearable
              @keyup.enter="searchArticles"
              @clear="searchArticles"
            />
            <button class="btn-ghost" :disabled="articleLoading" @click="searchArticles">搜索</button>
            <button v-if="hasActiveFilter" class="btn-ghost" :disabled="articleLoading" @click="resetFilters">重置</button>
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
          <h3>分类频道</h3>
          <el-input v-model="categoryKeyword" placeholder="搜索分类" clearable size="small" class="category-search" />
          <div class="category-list category-scroll">
            <button class="category-item category-button" :class="{ active: selectedCategoryId === null }" @click="selectCategory(null)">
              <span>全部文章</span><span>全部</span>
            </button>
            <button
              v-for="item in filteredCategories"
              :key="item.id"
              class="category-item category-button"
              :class="{ active: selectedCategoryId === item.id }"
              @click="selectCategory(item.id)"
            >
              <span>{{ item.name }}</span><span>#</span>
            </button>
          </div>
          <el-empty v-if="filteredCategories.length === 0" description="暂无匹配分类" :image-size="72" />
        </div>
        <div class="side-panel">
          <h3>标签墙</h3>
          <div class="tag-row">
            <button
              v-for="tag in tags"
              :key="tag.id"
              class="anime-tag tag-button"
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import PortalNav from '../../components/PortalNav.vue'
import ArticleCard from '../../components/ArticleCard.vue'
import { portalApi } from '../../api/blog'

const articles = ref([])
const categories = ref([])
const tags = ref([])
const hero = ref([])
const keyword = ref('')
const categoryKeyword = ref('')
const selectedCategoryId = ref(null)
const selectedTagId = ref(null)
const articleLoading = ref(false)
const articlePage = reactive({ current: 1, size: 9, total: 0 })

const heroUrl = computed(() => hero.value[0]?.url || '')
const heroStyle = computed(() => heroUrl.value ? { backgroundImage: `url(${heroUrl.value})` } : {})
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
  categories.value = res.data.categories || []
  tags.value = res.data.tags || []
  hero.value = res.data.hero || []
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

onMounted(async () => {
  await loadHomeMeta()
  await loadArticles()
})
</script>
