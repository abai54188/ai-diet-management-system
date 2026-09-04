<template>
  <el-container class="layout-container">
    <!-- 左侧导航栏 -->
    <el-aside width="212px" class="layout-aside">
      <!-- 品牌区: 叶片标记 + 名称 -->
      <div class="logo">
        <div class="logo-mark">
          <svg viewBox="0 0 24 24" fill="none" class="logo-svg">
            <path d="M12 21c-4.5-2-8-5.5-8-10 5.5-1.5 9 .5 11 3 2-2.5 5.5-4.5 11-3 0 4.5-3.5 8-8 10-1 .4-1 .4-2 0z" fill="var(--brand-500)" />
            <path d="M12 21c0-4 1.5-7.5 4-10" stroke="#fff" stroke-width="1.2" stroke-linecap="round" />
          </svg>
        </div>
        <div class="logo-text">
          <span class="logo-name">食在 AI</span>
          <span class="logo-sub">智能饮食管理</span>
        </div>
      </div>

      <!-- 导航菜单 (分组展示更易扫读) -->
      <div class="menu-scroll">
        <el-menu :default-active="activeMenu" router class="side-menu">
          <div class="menu-group">总览</div>
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon><span>首页</span>
          </el-menu-item>
          <div class="menu-group">营养工具</div>
          <el-menu-item index="/food/query">
            <el-icon><Food /></el-icon><span>营养与食谱</span>
          </el-menu-item>
          <div class="menu-group">健康管理</div>
          <el-menu-item index="/health/profile">
            <el-icon><User /></el-icon><span>健康档案</span>
          </el-menu-item>
          <el-menu-item index="/health/diet">
            <el-icon><ForkSpoon /></el-icon><span>饮食打卡</span>
          </el-menu-item>
          <el-menu-item index="/health/weight">
            <el-icon><TrendCharts /></el-icon><span>体重管理</span>
          </el-menu-item>
          <el-menu-item index="/health/analysis">
            <el-icon><DataAnalysis /></el-icon><span>健康分析</span>
          </el-menu-item>
          <el-menu-item index="/health/assistant">
            <el-icon><ChatDotRound /></el-icon><span>营养助手</span>
          </el-menu-item>
          <div class="menu-group">社区与数据</div>
          <el-menu-item index="/community/square">
            <el-icon><Share /></el-icon><span>社区广场</span>
          </el-menu-item>
          <el-menu-item index="/dashboard">
            <el-icon><DataLine /></el-icon><span>数据看板</span>
          </el-menu-item>
          <div v-if="isAdmin" class="menu-group">运营</div>
          <el-menu-item v-if="isAdmin" index="/admin/users">
            <el-icon><Setting /></el-icon><span>管理后台</span>
          </el-menu-item>
        </el-menu>
      </div>
    </el-aside>

    <el-container>
      <!-- 顶部用户栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <span class="header-crumb">AI 智能饮食管理系统</span>
          <span class="header-sep">/</span>
          <span class="header-title page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <!-- 日期问候 -->
          <div class="header-date">
            <el-icon><Calendar /></el-icon>
            <span>{{ todayText }}</span>
          </div>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="34" class="user-avatar avatar-brand">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              <el-tag v-if="isAdmin" type="danger" size="small" effect="plain">管理员</el-tag>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 (路由切换淡入上移) -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <div :key="route.path" class="page-enter">
            <component :is="Component" />
          </div>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 当前激活菜单(与路由路径保持一致)
const activeMenu = computed(() => route.path)

// 顶部页面标题
const pageTitle = computed(() => route.meta.title || '')

// 顶栏日期文本 (如: 09-04 · 周五)
const todayText = computed(() => {
  const d = new Date()
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${mm}-${dd} · ${week}`
})

// 头像文字(取昵称/用户名首字符)
const avatarText = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || ''
  return name.charAt(0).toUpperCase()
})

// 是否为管理员
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')

// 挂载时若无用户信息则拉取(如刷新页面后的恢复场景)
onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    userStore.refreshUserInfo()
  }
})

/**
 * 下拉菜单指令处理
 */
async function handleCommand(command) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
/* 整体布局铺满 */
.layout-container {
  height: 100%;
}

/* ---------- 左侧导航栏: 暖白底 ---------- */
.layout-aside {
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border-right: 1px solid var(--line-1);
  overflow: hidden;
}

/* 品牌区 */
.logo {
  height: 64px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid var(--line-1);
}

.logo-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--brand-50);
  border: 1px solid var(--brand-100);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-svg {
  width: 22px;
  height: 22px;
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.logo-name {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
  color: var(--brand-700);
  letter-spacing: 2px;
}

.logo-sub {
  font-size: 10px;
  color: var(--ink-400);
  letter-spacing: 1px;
}

/* 菜单滚动区 */
.menu-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 8px 10px 16px;
}

/* 菜单组标题 */
.menu-group {
  padding: 14px 10px 6px;
  font-size: 10.5px;
  letter-spacing: 2px;
  color: var(--ink-400);
  text-transform: uppercase;
}

/* 菜单项: 圆角块 + 激活苔绿 */
.side-menu {
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--ink-600);
  --el-menu-hover-bg-color: var(--bg-hover);
  --el-menu-active-color: var(--brand-600);
}

.side-menu :deep(.el-menu-item) {
  height: 42px;
  margin: 2px 0;
  border-radius: 10px;
  font-size: 13.5px;
  transition: background-color var(--dur-fast) ease, color var(--dur-fast) ease;
}

.side-menu :deep(.el-menu-item:hover) {
  background: var(--bg-hover);
  color: var(--ink-900);
}

.side-menu :deep(.el-menu-item.is-active) {
  background: var(--brand-100);
  color: var(--brand-700);
  font-weight: 600;
}

.side-menu :deep(.el-menu-item.is-active .el-icon) {
  color: var(--brand-600);
}

/* ---------- 顶部栏 ---------- */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--line-1);
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.header-crumb {
  font-size: 12px;
  color: var(--ink-400);
}

.header-sep {
  color: var(--line-2);
  font-size: 12px;
}

.header-title {
  font-size: 19px;
  font-weight: 700;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 22px;
}

/* 日期胶囊 */
.header-date {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--ink-600);
  background: var(--bg-paper);
  border: 1px solid var(--line-1);
  border-radius: 999px;
  padding: 5px 14px;
}

/* 用户信息区 */
.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 8px;
}

.user-name {
  color: var(--ink-900);
  font-size: 13.5px;
  font-weight: 500;
}

/* ---------- 主内容区 ---------- */
.layout-main {
  background: transparent;
  padding: 20px 22px 26px;
}
</style>