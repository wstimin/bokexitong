<template>
  <section>
    <div class="stats">
      <div v-for="item in statItems" :key="item.label" class="stat-card">
        <span>{{ item.label }}</span><strong>{{ item.value }}</strong>
      </div>
    </div>
    <div class="admin-card">
      <div ref="trendRef" style="height: 320px"></div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { adminApi } from '../../api/blog'

const stats = ref({})
const trendRef = ref()
const statItems = computed(() => [
  { label: '文章', value: stats.value.articleCount || 0 },
  { label: '用户', value: stats.value.userCount || 0 },
  { label: '评论', value: stats.value.commentCount || 0 },
  { label: '图片链接', value: stats.value.imageCount || 0 }
])

onMounted(async () => {
  const res = await adminApi.dashboard()
  stats.value = res.data
  const chart = echarts.init(trendRef.value)
  chart.setOption({
    tooltip: {},
    xAxis: { type: 'category', data: (stats.value.publishTrend || []).map((i) => i.date) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, areaStyle: {}, data: (stats.value.publishTrend || []).map((i) => i.count), color: '#ff77b7' }]
  })
})
</script>
