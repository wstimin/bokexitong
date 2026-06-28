<template>
  <div class="page">
    <PortalNav />
    <main class="shell detail">
      <article class="detail-article">
        <img v-if="article.coverUrl" class="cover" :src="article.coverUrl" :alt="article.title" />
        <div class="meta" style="margin-top: 18px">
          <span>{{ article.publishedAt || article.createdAt }}</span>
          <span>{{ article.viewCount || 0 }} 浏览</span>
          <span>{{ article.likeCount || 0 }} 点赞</span>
          <span>{{ article.favoriteCount || 0 }} 收藏</span>
        </div>
        <h1>{{ article.title }}</h1>
        <div class="hero-actions" style="margin-bottom: 22px">
          <button class="btn-primary" @click="like">点赞</button>
          <button class="btn-ghost" @click="favorite">收藏</button>
        </div>
        <div class="markdown" v-html="html"></div>
      </article>

      <section class="detail-article" style="margin-top: 18px">
        <h2>评论区</h2>
        <el-input v-model="commentText" type="textarea" :rows="3" placeholder="写下你的评论" />
        <button class="btn-primary" style="margin-top: 12px" @click="submitComment">发布评论</button>
        <div v-for="item in comments" :key="item.id" class="side-panel" style="margin-top: 12px">
          {{ item.content }}
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import hljs from 'highlight.js'
import PortalNav from '../../components/PortalNav.vue'
import { articleApi, commentApi, portalApi } from '../../api/blog'

marked.use({
  renderer: {
    code(code, lang) {
      const language = hljs.getLanguage(lang) ? lang : 'plaintext'
      return `<pre><code>${hljs.highlight(code, { language }).value}</code></pre>`
    }
  }
})

const route = useRoute()
const article = ref({})
const comments = ref([])
const commentText = ref('')
const html = computed(() => marked(article.value.content || ''))

const load = async () => {
  const [detail, commentPage] = await Promise.all([
    portalApi.detail(route.params.id),
    commentApi.page({ articleId: route.params.id })
  ])
  article.value = detail.data
  comments.value = commentPage.data.records || []
}

const like = () => articleApi.like(route.params.id)
const favorite = () => articleApi.favorite(route.params.id)
const submitComment = async () => {
  await commentApi.save({ articleId: route.params.id, content: commentText.value })
  commentText.value = ''
  load()
}

onMounted(load)
</script>
