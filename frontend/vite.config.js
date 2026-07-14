import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('element-plus')) return 'element-plus'
          if (id.includes('echarts')) return 'echarts'
          if (id.includes('marked') || id.includes('highlight.js')) return 'editor-render'
          if (id.includes('@element-plus/icons-vue') || id.includes('lucide-vue-next')) return 'icons'
          if (id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) return 'vue-vendor'
          return 'vendor'
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
