<template>
  <div class="page">
    <PortalNav />
    <main class="shell detail">
      <section class="detail-article">
        <h1>用户中心</h1>
        <p>这里用于维护个人资料、头像 URL、密码重置，以及查看我的点赞和收藏记录。</p>
        <el-form label-position="top" style="max-width: 520px">
          <el-form-item label="昵称"><el-input :model-value="auth.user?.nickname" /></el-form-item>
          <el-form-item label="头像 URL"><el-input :model-value="auth.user?.avatar" placeholder="https://example.com/avatar.png" /></el-form-item>
          <el-form-item label="邮箱"><el-input :model-value="auth.user?.email" /></el-form-item>
        </el-form>
      </section>
    </main>
  </div>
</template>

<script setup>
import PortalNav from '../../components/PortalNav.vue'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
</script>
