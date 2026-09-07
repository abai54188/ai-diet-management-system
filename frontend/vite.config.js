import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    // 配置 @ 指向 src 目录
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    // 前端开发服务器端口
    port: 5173,
    proxy: {
      // 将 /api 开头的请求代理到后端 8080，规避跨域
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    // 一体化部署: 前端构建产物直接输出到后端 Spring Boot 的静态资源目录
    // 打包进 jar 后由后端同一端口托管, 无需单独部署前端
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true
  }
})