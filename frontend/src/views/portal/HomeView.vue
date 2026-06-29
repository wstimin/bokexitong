<template>
  <div class="page">
    <PortalNav />
    <section class="hero" :class="{ 'hero-fallback': !heroUrl }" :style="heroStyle">
      <div class="shell hero-content">
        <span class="eyebrow">Anime Style Personal Blog</span>
        <h1>星绘 Blog</h1>
        <p>这里展示最新发布的文章。登录后的用户可以在用户中心创作内容、维护资料和修改密码。</p>
      </div>
    </section>

    <main class="shell main-grid">
      <section>
        <div class="section-title">
          <h2>最新文章</h2>
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
