<template>
  <div class="page">
    <PortalNav />
    <header class="page-hero compact-hero">
      <div class="shell">
        <span class="eyebrow">ARCHIVE · COLLECTION</span>
        <h1>{{ pageTitle }}</h1>
        <p>按分类、标签或关键词，重新发现那些值得阅读的内容。</p>
      </div>
    </header>

    <main class="shell archive-layout">
      <aside class="archive-sidebar">
        <div class="side-panel">
          <span class="section-kicker">CATEGORIES</span>
          <h3>文章分类</h3>
          <button type="button" :class="['archive-filter', { active: !categoryId }]" @click="selectCategory(null)">
            <span>全部文章</span><strong>{{ page.total }}</strong>
          </button>
          <button v-for="item in categories" :key="item.id" type="button" :class="['archive-filter', { active: categoryId === item.id }]" @click="selectCategory(item.id)">
            <span>{{ item.name }}</span><span>→</span>
          </button>
        </div>
        <div class="side-panel">
          <span class="section-kicker">TAGS</span>
          <h3>标签索引</h3>
          <div class="tag-row">
            <button v-for="tag in tags" :key="tag.id" type="button" :class="['tag-button', 'badge-tag', { active: tagId === tag.id }]" @click="selectTag(tag.id)">{{ tag.name }}</button>
          </div>
        </div>
      </aside>

      <section class="archive-content">
        <div class="archive-toolbar">
          <div><span class="section-kicker">ALL POSTS</span><h2>{{ resultTitle }}</h2></div>
          <div class="archive-search">
            <el-input v-model="keyword" placeholder="搜索文章" clearable @keyup.enter="submitSearch" @clear="submitSearch" />
            <button class="btn-primary" type="button" @click="submitSearch">搜索</button>
          </div>
        </div>
        <div v-loading="loading" class="article-grid archive-grid">
          <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
        </div>
        <el-empty v-if="!loading && !articles.length" description="没有找到相关文章" />
        <div v-if="page.total > page.size" class="pager">
          <el-pagination v-model:current-page="page.current" background layout="prev, pager, next" :total="page.total" :page-size="page.size" @current-change="load" />
        </div>
      </section>
    </main>
    <PortalFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ArticleCard from '../../components/ArticleCard.vue'
import PortalFooter from '../../components/PortalFooter.vue'
import PortalNav from '../../components/PortalNav.vue'
import { portalApi } from '../../api/blog'

const route = useRoute()
const router = useRouter()
const articles = ref([])
const categories = ref([])
const tags = ref([])
const keyword = ref(String(route.query.keyword || ''))
const categoryId = ref(route.query.category ? Number(route.query.category) : null)
const tagId = ref(route.query.tag ? Number(route.query.tag) : null)
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

const activeCategory = computed(() => categories.value.find((item) => item.id === categoryId.value))
const activeTag = computed(() => tags.value.find((item) => item.id === tagId.value))
const pageTitle = computed(() => route.query.view === 'categories' ? '分类浏览' : '文章归档')
const resultTitle = computed(() => activeCategory.value?.name || activeTag.value?.name || (keyword.value ? `“${keyword.value}”的结果` : '全部文章'))

const syncQuery = () => router.replace({ query: {
  ...(route.query.view ? { view: route.query.view } : {}),
  ...(categoryId.value ? { category: categoryId.value } : {}),
  ...(tagId.value ? { tag: tagId.value } : {}),
  ...(keyword.value.trim() ? { keyword: keyword.value.trim() } : {})
} })
const load = async () => {
  loading.value = true
  try {
    const res = await portalApi.articles({ current: page.current, size: page.size, keyword: keyword.value.trim() || undefined, categoryId: categoryId.value || undefined, tagId: tagId.value || undefined })
    articles.value = res.data.records || []
    page.total = res.data.total || 0
  } finally { loading.value = false }
}
const selectCategory = (id) => { categoryId.value = id; tagId.value = null; page.current = 1; syncQuery(); load() }
const selectTag = (id) => { tagId.value = tagId.value === id ? null : id; categoryId.value = null; page.current = 1; syncQuery(); load() }
const submitSearch = () => { page.current = 1; syncQuery(); load() }

watch(() => route.query.view, () => {})
onMounted(async () => {
  const meta = await portalApi.taxonomies()
  categories.value = meta.data.categories || []
  tags.value = meta.data.tags || []
  await load()
})
</script>
