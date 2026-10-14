<template>
  <Teleport to="body">
    <Transition name="search-fade">
      <div v-if="modelValue" class="search-overlay" role="dialog" aria-modal="true" aria-label="搜索文章" @click.self="close">
        <section class="search-panel">
          <div class="search-panel__head">
            <div>
              <span class="overline">DISCOVER</span>
              <h2>搜索文章</h2>
            </div>
            <button class="icon-button" type="button" aria-label="关闭搜索" @click="close">×</button>
          </div>
          <div class="global-search-field">
            <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>
            <input ref="inputRef" v-model="keyword" type="search" placeholder="输入标题、摘要或关键词…" @input="scheduleSearch" @keyup.enter="search" />
            <kbd>ESC</kbd>
          </div>
          <div class="search-panel__body" v-loading="loading">
            <p v-if="!keyword.trim()" class="search-hint">输入关键词，在全部公开文章中查找内容。</p>
            <template v-else-if="results.length">
              <RouterLink v-for="item in results" :key="item.id" class="search-result" :to="`/article/${item.id}`" @click="close">
                <span>{{ item.categoryName || '未分类' }}</span>
                <strong>{{ item.title }}</strong>
                <p>{{ item.summary || '阅读全文查看完整内容。' }}</p>
              </RouterLink>
            </template>
            <div v-else-if="searched && !loading" class="search-empty">没有找到与“{{ keyword }}”相关的文章</div>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { portalApi } from '../api/blog'

const props = defineProps({ modelValue: { type: Boolean, default: false } })
const emit = defineEmits(['update:modelValue'])
const keyword = ref('')
const results = ref([])
const loading = ref(false)
const searched = ref(false)
const inputRef = ref()
let timer

const close = () => emit('update:modelValue', false)
const search = async () => {
  const value = keyword.value.trim()
  if (!value) {
    results.value = []
    searched.value = false
    return
  }
  loading.value = true
  try {
    const res = await portalApi.articles({ current: 1, size: 8, keyword: value })
    results.value = res.data.records || []
    searched.value = true
  } finally {
    loading.value = false
  }
}
const scheduleSearch = () => {
  window.clearTimeout(timer)
  timer = window.setTimeout(search, 280)
}
const onKeydown = (event) => { if (event.key === 'Escape' && props.modelValue) close() }

watch(() => props.modelValue, async (open) => {
  document.body.classList.toggle('overlay-open', open)
  if (open) {
    await nextTick()
    inputRef.value?.focus()
  }
})

window.addEventListener('keydown', onKeydown)
onBeforeUnmount(() => {
  window.clearTimeout(timer)
  window.removeEventListener('keydown', onKeydown)
  document.body.classList.remove('overlay-open')
})
</script>
