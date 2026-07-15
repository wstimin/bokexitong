<template>
  <section>
    <div class="stats dashboard-stats">
      <div v-for="item in statItems" :key="item.label" class="stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>

    <div class="dashboard-grid">
      <section class="admin-card dashboard-shortcuts">
        <div class="chart-title">
          <h2>常用操作</h2>
        </div>
        <div class="shortcut-grid">
          <RouterLink v-for="item in shortcuts" :key="item.to" class="shortcut-item" :to="item.to">
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </RouterLink>
        </div>
      </section>

      <section class="admin-card">
        <div class="chart-title">
          <h2>近 7 天发布趋势</h2>
        </div>
        <div ref="trendRef" class="chart-box"></div>
      </section>

      <section class="admin-card">
        <div class="chart-title">
          <h2>文章状态分布</h2>
        </div>
        <div ref="statusRef" class="chart-box"></div>
      </section>

      <section class="admin-card">
        <div class="chart-title">
          <h2>分类文章占比</h2>
        </div>
        <div ref="categoryRef" class="chart-box"></div>
      </section>

      <section class="admin-card dashboard-alerts">
        <div class="chart-title">
          <h2>待处理事项</h2>
        </div>
        <div class="todo-list">
          <RouterLink to="/admin/articles" class="todo-item">
            <span>待审核文章</span>
            <strong>{{ stats.pendingArticleCount || 0 }}</strong>
          </RouterLink>
          <RouterLink to="/admin/comments" class="todo-item">
            <span>待审核评论</span>
            <strong>{{ stats.pendingCommentCount || 0 }}</strong>
          </RouterLink>
          <RouterLink to="/admin/articles" class="todo-item">
            <span>已驳回文章</span>
            <strong>{{ stats.rejectedArticleCount || 0 }}</strong>
          </RouterLink>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { adminApi } from '../../api/blog'

use([LineChart, PieChart, GridComponent, TooltipComponent, CanvasRenderer])

const stats = ref({})
const trendRef = ref()
const statusRef = ref()
const categoryRef = ref()
const charts = []

const statItems = computed(() => [
  { label: '文章总数', value: stats.value.articleCount || 0 },
  { label: '用户总数', value: stats.value.userCount || 0 },
  { label: '评论总数', value: stats.value.commentCount || 0 },
  { label: '图片资源', value: stats.value.imageCount || 0 },
  { label: '点赞总数', value: stats.value.likeCount || 0 },
  { label: '收藏总数', value: stats.value.favoriteCount || 0 },
  { label: '待审文章', value: stats.value.pendingArticleCount || 0 },
  { label: '已发布文章', value: stats.value.publishedArticleCount || 0 }
])

const shortcuts = [
  { title: '站点设置', desc: '修改站点名称、首页文案和背景', to: '/admin/settings' },
  { title: '邮箱设置', desc: '配置 SMTP、发件名称和测试验证码邮件', to: '/admin/mail-settings' },
  { title: '图片资源', desc: '维护 Logo、横幅、封面和素材', to: '/admin/images' },
  { title: '文章审核', desc: '审核、下架或删除用户文章', to: '/admin/articles' },
  { title: '分类标签', desc: '整理文章分类和标签体系', to: '/admin/taxonomies' },
  { title: '评论审核', desc: '处理待审评论和违规内容', to: '/admin/comments' },
  { title: '用户权限', desc: '管理用户角色、状态和密码', to: '/admin/users' }
]

const initChart = (el, option) => {
  if (!el) return
  const chart = init(el)
  chart.setOption(option)
  charts.push(chart)
}

const renderCharts = () => {
  const trend = stats.value.publishTrend || []
  const statusPie = stats.value.statusPie || []
  const categoryPie = stats.value.categoryPie || []

  initChart(trendRef.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 18, top: 28, bottom: 34 },
    xAxis: { type: 'category', data: trend.map((item) => item.date), boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ name: '发布数', type: 'line', smooth: true, areaStyle: {}, data: trend.map((item) => item.count), color: '#2f80ed' }]
  })

  initChart(statusRef.value, {
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['48%', '72%'], data: statusPie, label: { formatter: '{b}: {c}' } }]
  })

  initChart(categoryRef.value, {
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '70%', data: categoryPie, label: { formatter: '{b}: {c}' } }]
  })
}

const resizeCharts = () => charts.forEach((chart) => chart.resize())

onMounted(async () => {
  try {
    const res = await adminApi.dashboard()
    stats.value = res.data || {}
    await nextTick()
    renderCharts()
    window.addEventListener('resize', resizeCharts)
  } catch (error) {
    console.error(error)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  charts.forEach((chart) => chart.dispose())
})
</script>
