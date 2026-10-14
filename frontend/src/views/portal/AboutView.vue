<template>
  <div class="page">
    <PortalNav />
    <main class="about-page">
      <section class="about-hero shell">
        <div class="about-portrait" aria-hidden="true"><span>{{ site.name.slice(0, 1) }}</span></div>
        <div>
          <span class="eyebrow">ABOUT · THE JOURNAL</span>
          <h1>关于 {{ site.name }}</h1>
          <p>{{ site.heroSubtitle }}</p>
          <div class="hero-actions">
            <RouterLink class="btn-primary" to="/archive">阅读文章</RouterLink>
            <a class="btn-ghost" href="#contact">保持联系</a>
          </div>
        </div>
      </section>

      <section class="shell about-grid">
        <article class="about-story">
          <span class="section-kicker">OUR STORY</span>
          <h2>记录思考，也记录时间。</h2>
          <p>这里是一个专注于长期价值的个人内容空间。我们相信清晰的表达、克制的设计和持续的好奇心，能够让互联网重新变得温暖而有深度。</p>
          <p>文章围绕技术、设计、生活与创作展开。没有喧闹的信息流，只有经过整理的观点与经验。</p>
        </article>
        <div class="interest-grid">
          <article v-for="item in interests" :key="item.title" class="interest-card">
            <span>{{ item.index }}</span><h3>{{ item.title }}</h3><p>{{ item.description }}</p>
          </article>
        </div>
      </section>

      <section id="contact" class="shell contact-panel">
        <div><span class="section-kicker">CONTACT</span><h2>让有价值的对话发生。</h2></div>
        <div v-if="safeContactHtml" class="portal-contact-html" v-html="safeContactHtml"></div>
        <p v-else>你可以通过文章评论与我们交流，也可以在站点后台配置更多联系方式。</p>
      </section>
    </main>
    <PortalFooter />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import PortalFooter from '../../components/PortalFooter.vue'
import PortalNav from '../../components/PortalNav.vue'
import { useSiteStore } from '../../stores/site'
import { sanitizeContactHtml } from '../../utils/sanitize'

const site = useSiteStore()
const safeContactHtml = computed(() => sanitizeContactHtml(site.contactHtml).trim())
const interests = [
  { index: '01', title: '技术与创造', description: '探索工具、工程与创造性解决问题的方式。' },
  { index: '02', title: '设计与体验', description: '关注界面之外，那些让产品真正好用的细节。' },
  { index: '03', title: '阅读与思考', description: '把零散的信息整理为值得长期保留的知识。' },
  { index: '04', title: '生活与远方', description: '在快节奏之外，记录真实、安静而具体的生活。' }
]
</script>
