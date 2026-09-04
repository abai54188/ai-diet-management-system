<template>
  <el-container class="admin-container">
    <!-- 左侧管理菜单 -->
    <el-aside width="200px" class="admin-aside">
      <el-menu :default-active="activeMenu" router background-color="var(--brand-800)" text-color="#bfcbd9"
        active-text-color="#8FC7AA">
        <div class="admin-logo">管理后台</div>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/content">
          <el-icon><Document /></el-icon><span>内容管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/food">
          <el-icon><Food /></el-icon><span>食材库管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/stats">
          <el-icon><DataAnalysis /></el-icon><span>数据统计</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <!-- 右侧内容区 -->
    <el-main class="admin-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.admin-container {
  height: 100%;
  background: #f0f2f5;
}

.admin-aside {
  background: var(--brand-800);
}

.admin-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 17px;
  font-weight: bold;
  letter-spacing: 4px;
}

.admin-main {
  padding: 16px;
}
</style>