import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 基础路由配置
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页', requiresAuth: true }
      },
      {
        path: 'food/query',
        name: 'FoodQuery',
        component: () => import('@/views/food/query.vue'),
        meta: { title: '营养与食谱', requiresAuth: true }
      },
      {
        path: 'recipe/generate',
        name: 'RecipeGenerate',
        component: () => import('@/views/recipe/generate.vue'),
        meta: { title: '食谱生成', requiresAuth: true }
      },
      {
        path: 'health/profile',
        name: 'HealthProfile',
        component: () => import('@/views/health/profile.vue'),
        meta: { title: '健康档案', requiresAuth: true }
      },
      {
        path: 'health/diet',
        name: 'HealthDiet',
        component: () => import('@/views/health/diet.vue'),
        meta: { title: '饮食打卡', requiresAuth: true }
      },
      {
        path: 'health/weight',
        name: 'HealthWeight',
        component: () => import('@/views/health/weight.vue'),
        meta: { title: '体重管理', requiresAuth: true }
      },
      {
        path: 'health/analysis',
        name: 'HealthAnalysis',
        component: () => import('@/views/health/analysis.vue'),
        meta: { title: '健康分析', requiresAuth: true }
      },
      {
        path: 'health/assistant',
        name: 'HealthAssistant',
        component: () => import('@/views/health/chat.vue'),
        meta: { title: '营养助手', requiresAuth: true }
      },
      {
        path: 'community/square',
        name: 'CommunitySquare',
        component: () => import('@/views/community/square.vue'),
        meta: { title: '社区广场', requiresAuth: true }
      },
      {
        path: 'community/publish',
        name: 'CommunityPublish',
        component: () => import('@/views/community/publish.vue'),
        meta: { title: '发布动态', requiresAuth: true }
      },
      {
        path: 'community/profile/:userId',
        name: 'CommunityProfile',
        component: () => import('@/views/community/profile.vue'),
        meta: { title: '个人主页', requiresAuth: true }
      },
      {
        path: 'community/favorites',
        name: 'CommunityFavorites',
        component: () => import('@/views/community/favorites.vue'),
        meta: { title: '食谱收藏夹', requiresAuth: true }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据看板', requiresAuth: true }
      },
      {
        path: 'admin',
        component: () => import('@/views/admin/index.vue'),
        redirect: '/admin/users',
        meta: { requiresAuth: true, requiresAdmin: true },
        children: [
          { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/users.vue'), meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true } },
          { path: 'content', name: 'AdminContent', component: () => import('@/views/admin/content.vue'), meta: { title: '内容管理', requiresAuth: true, requiresAdmin: true } },
          { path: 'food', name: 'AdminFood', component: () => import('@/views/admin/food.vue'), meta: { title: '食材库管理', requiresAuth: true, requiresAdmin: true } },
          { path: 'stats', name: 'AdminStats', component: () => import('@/views/admin/stats.vue'), meta: { title: '数据统计', requiresAuth: true, requiresAdmin: true } }
        ]
      }
    ]
  },
  // 兜底路由: 未匹配的路径统一跳转首页
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  // base 读取构建配置: 一体化部署为 '/', GitHub Pages 子路径部署时为 '/仓库名/'
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 全局前置守卫: 登录态校验
router.beforeEach((to) => {
  // 演示模式(GitHub Pages 静态展示): 跳过登录校验, 允许访客直接浏览各页面
  if (import.meta.env.VITE_DEMO_MODE === 'true') {
    return true
  }
  const userStore = useUserStore()
  // 访问需要登录的页面但未登录 -> 跳转登录页
  if (to.meta.requiresAuth && !userStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 管理员页面角色校验
  if (to.meta.requiresAdmin && userStore.userInfo?.role !== 'ADMIN') {
    return { path: '/home' }
  }
  // 已登录访问登录页 -> 跳转首页
  if (to.path === '/login' && userStore.token) {
    return { path: '/home' }
  }
})

// 动态设置页面标题
router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - AI 智能饮食管理系统` : 'AI 智能饮食管理系统'
})

export default router