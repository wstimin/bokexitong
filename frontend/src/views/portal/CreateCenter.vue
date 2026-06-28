<template>
  <div class="page">
    <PortalNav />
    <main class="shell editor-grid">
      <section class="form-panel">
        <div class="section-title"><h2>创作中心</h2><span class="anime-tag">Markdown / 富文本</span></div>
        <el-form label-position="top">
          <el-form-item label="文章标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" /></el-form-item>
          <el-form-item label="封面图片 URL"><el-input v-model="form.coverUrl" placeholder="https://example.com/cover.jpg" /></el-form-item>
          <el-form-item label="正文 Markdown"><el-input v-model="form.content" type="textarea" :rows="16" /></el-form-item>
        </el-form>
        <button class="btn-primary" @click="save('PUBLISHED')">发布</button>
        <button class="btn-ghost" style="margin-left: 10px" @click="save('DRAFT')">存草稿</button>
      </section>
      <aside class="form-panel">
        <h3>封面预览</h3>
        <img v-if="form.coverUrl" class="cover" :src="form.coverUrl" alt="封面预览" />
        <div v-else class="cover"></div>
        <h3>创作提示</h3>
        <p>封面、插图、横幅都使用 URL 字段保存，你可以在后台图片管理里集中维护素材链接。</p>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import PortalNav from '../../components/PortalNav.vue'
import { articleApi } from '../../api/blog'

const form = reactive({ title: '', summary: '', coverUrl: '', content: '', contentType: 'MARKDOWN' })
const save = async (status) => {
  await articleApi.save({ ...form, status })
  ElMessage.success(status === 'PUBLISHED' ? '发布成功' : '草稿已保存')
}
</script>
