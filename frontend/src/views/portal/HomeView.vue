<template>
  <div class="page">
    <PortalNav />
    <section class="hero" :class="{ 'hero-fallback': !heroUrl }" :style="heroStyle">
      <div class="shell hero-content">
        <span class="eyebrow">Anime Style Personal Blog</span>
        <h1>星绘 Blog</h1>
        <p>一个二次元风格的个人博客系统，支持文章、分类、标签、评论、点赞、收藏、创作中心和后台数据看板。</p>
        <div class="hero-actions">
          <RouterLink class="btn-primary" to="/create">开始创作</RouterLink>
        </div>
      </div>
    </section>

    <main class="shell main-grid">
      <section>
        <div class="section-title">
          <h2>最新文章</h2>
          <RouterLink class="nav-link" to="/create">发布文章</RouterLink>
        </div>
        <div class="article-grid">
          <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
        </div>
      </section>

      <aside>
        <div class="side-panel">
          <h3>分类频道</h3>
          <div class="category-list">
            <div v-for="item in categories" :key="item.id" class="category-item">
              <span>{{ item.name }}</span><span>#</span>
            </div>
          </div>
        </div>
        <div class="side-panel">
          <h3>标签云</h3>
          <div class="tag-row">
            <span v-for="tag in tags" :key="tag.id" class="anime-tag" :style="{ color: tag.color }">{{ tag.name }}</span>
          </div>
        </div>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import PortalNav from '../../components/PortalNav.vue'
import ArticleCard from '../../components/ArticleCard.vue'
import { portalApi } from '../../api/blog'

const articles = ref([])
const categories = ref([])
const tags = ref([])
const hero = ref([])
const heroUrl = computed(() => hero.value[0]?.url || '')
const heroStyle = computed(() => heroUrl.value ? { backgroundImage: `url(${heroUrl.value})` } : {})

onMounted(async () => {
  const res = await portalApi.home()
  articles.value = res.data.articles || []
  categories.value = res.data.categories || []
  tags.value = res.data.tags || []
  hero.value = res.data.hero || []
})
</script>
