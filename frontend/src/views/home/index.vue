<template>
  <div class="home-page">
    <!-- 欢迎横幅: 品牌绿渐变 + 衬线问候 -->
    <el-card shadow="never" class="welcome-card page-enter">
      <div class="welcome-content">
        <div class="welcome-text">
          <div class="welcome-eyebrow">{{ todayText }}</div>
          <h2 class="welcome-title">{{ greeting }}，{{ displayName }}</h2>
          <p class="welcome-sub">
            {{ goalText }} — 今天已摄入
            <b class="num hl">{{ summary?.totalCalorie ?? '0' }}</b> kcal，
            距推荐还有
            <b class="num hl">{{ remaining }}</b> kcal
          </p>
        </div>
        <!-- 装饰叶片 -->
        <svg viewBox="0 0 24 24" class="welcome-leaf">
          <path d="M12 21c-4.5-2-8-5.5-8-10 5.5-1.5 9 .5 11 3 2-2.5 5.5-4.5 11-3 0 4.5-3.5 8-8 10-1 .4-1 .4-2 0z" fill="rgba(255,255,255,0.22)" />
        </svg>
      </div>
    </el-card>

    <!-- 今日速览三卡 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="8" v-for="s in statCards" :key="s.label">
        <el-card shadow="never" class="stat-card card-hover">
          <div class="stat-inner">
            <div class="stat-icon" :class="s.tint">
              <el-icon :size="22"><component :is="s.icon" /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value num">{{ s.value }}<span class="stat-unit">{{ s.unit }}</span></div>
              <div class="stat-label">{{ s.label }}</div>
              <div class="stat-tip">{{ s.tip }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card shadow="never" class="quick-card">
      <template #header>快捷入口</template>
      <div class="quick-grid">
        <div v-for="q in quickLinks" :key="q.title" class="quick-item card-hover" @click="$router.push(q.path)">
          <div class="quick-icon" :class="q.tint">
            <el-icon :size="20"><component :is="q.icon" /></el-icon>
          </div>
          <div class="quick-title">{{ q.title }}</div>
          <div class="quick-desc">{{ q.desc }}</div>
        </div>
      </div>
    </el-card>

    <!-- 图表占位: 接入真实数据前的骨架 -->
    <el-card shadow="never" class="chart-card">
      <template #header>近7日热量摄入趋势</template>
      <div class="chart-placeholder">
        <div class="empty-ring">
          <el-icon :size="30" color="var(--brand-400)"><TrendCharts /></el-icon>
        </div>
        <p class="empty-text">连续打卡几天后，这里将展示你的热量趋势曲线</p>
        <el-button type="primary" plain size="small" @click="$router.push('/dashboard')">前往数据看板</el-button>
      </div>
    </el-card>

    <p class="home-foot">本系统提供的营养数据与建议仅供饮食参考，不构成医疗建议</p>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { dailySummaryApi } from '@/api/health'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 今日摄入汇总(真实数据)
const summary = ref(null)

// 展示名称
const displayName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '朋友')

// 日期文本
const todayText = computed(() => {
  const d = new Date()
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  return `${d.getMonth() + 1}月${d.getDate()}日 · ${week}`
})

// 问候语
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

// 健康目标文案
const goalText = computed(() => ({
  LOSE: '减脂进行中',
  GAIN: '增重进行中'
}[userStore.userInfo?.healthGoal] || '均衡饮食维持中'))

// 距推荐剩余(kcal)
const remaining = computed(() => {
  if (!summary.value?.recommendCalorie) return '--'
  const diff = Number(summary.value.recommendCalorie) - Number(summary.value.totalCalorie || 0)
  return diff > 0 ? diff.toFixed(0) : '已达标'
})

// 三张速览卡
const statCards = computed(() => [
  {
    label: '今日已摄入',
    unit: ' kcal',
    value: summary.value?.totalCalorie ?? '--',
    tip: `蛋白质 ${summary.value?.totalProtein ?? '--'}g`,
    icon: 'ForkSpoon',
    tint: 'tint-cal'
  },
  {
    label: '每日推荐',
    unit: ' kcal',
    value: summary.value?.recommendCalorie ?? '--',
    tip: 'Mifflin-St Jeor 公式计算',
    icon: 'Aim',
    tint: 'tint-brand'
  },
  {
    label: '当前体重',
    unit: ' kg',
    value: userStore.userInfo?.weight ?? '--',
    tip: userStore.userInfo?.targetWeight ? `目标 ${userStore.userInfo.targetWeight}kg` : '未设置目标',
    icon: 'TrendCharts',
    tint: 'tint-fat'
  }
])

// 快捷入口
const quickLinks = [
  { title: '营养查询', desc: '1010 种食材 · 热量计算', path: '/food/query', icon: 'Food', tint: 'tint-cal' },
  { title: '饮食打卡', desc: 'AI 识别菜名 · 一键记录', path: '/health/diet', icon: 'ForkSpoon', tint: 'tint-protein' },
  { title: 'AI 食谱', desc: '食材匹配 · 一周规划', path: '/recipe/generate', icon: 'MagicStick', tint: 'tint-brand' },
  { title: '营养助手', desc: '结合档案的个性化问答', path: '/health/assistant', icon: 'ChatDotRound', tint: 'tint-carb' },
  { title: '社区广场', desc: '分享你的健康餐桌', path: '/community/square', icon: 'Share', tint: 'tint-protein' },
  { title: '数据看板', desc: '趋势 · 占比 · 达标率', path: '/dashboard', icon: 'DataLine', tint: 'tint-fat' }
]

onMounted(async () => {
  // 拉取今日真实汇总(档案未完善时静默降级)
  try {
    const res = await dailySummaryApi()
    summary.value = res.data
  } catch (e) {
    summary.value = null
  }
})
</script>

<style scoped>
/* ---------- 欢迎横幅 ---------- */
.welcome-card {
  margin-bottom: 16px;
  border: none;
  background: linear-gradient(120deg, var(--brand-600) 0%, var(--brand-500) 58%, var(--brand-400) 100%);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.welcome-card :deep(.el-card__body) {
  padding: 28px 32px;
}

.welcome-content {
  position: relative;
  display: flex;
  align-items: center;
}

.welcome-eyebrow {
  font-size: 12px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 8px;
}

.welcome-title {
  font-family: var(--font-display);
  color: #fff;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.welcome-sub {
  color: rgba(255, 255, 255, 0.88);
  font-size: 13.5px;
}

.welcome-sub .hl {
  color: #fff;
  font-size: 16px;
}

/* 装饰叶片: 右侧大面积淡出 */
.welcome-leaf {
  position: absolute;
  right: 28px;
  top: 50%;
  transform: translateY(-50%) rotate(-8deg);
  width: 150px;
  height: 150px;
}

/* ---------- 速览卡: 玻璃拟态 ---------- */
.stat-row {
  margin-bottom: 16px;
}

.stat-row .stat-card {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur)) saturate(1.15);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(1.15);
  border: 1px solid var(--glass-border);
  box-shadow: var(--ring-highlight), var(--shadow-card) !important;
}

.stat-row .stat-card:hover {
  box-shadow: var(--ring-highlight), var(--shadow-card-hover) !important;
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 500;
  color: var(--ink-900);
  line-height: 1.1;
}

.stat-unit {
  font-size: 12px;
  color: var(--ink-400);
  margin-left: 4px;
  font-family: var(--font-body);
}

.stat-label {
  font-size: 13px;
  color: var(--ink-600);
  margin-top: 3px;
}

.stat-tip {
  font-size: 11.5px;
  color: var(--ink-400);
  margin-top: 2px;
}

/* ---------- 快捷入口 ---------- */
.quick-card {
  margin-bottom: 16px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.quick-item {
  cursor: pointer;
  border: 1px solid var(--line-1);
  border-radius: var(--radius-md);
  padding: 14px 12px;
  text-align: center;
  background: var(--bg-card);
}

.quick-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.quick-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink-900);
}

.quick-desc {
  font-size: 11px;
  color: var(--ink-400);
  margin-top: 3px;
  white-space: nowrap;
}

/* ---------- 图表占位 ---------- */
.chart-card {
  min-height: 260px;
}

.chart-placeholder {
  height: 170px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.empty-ring {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: var(--brand-50);
  border: 1px dashed var(--brand-200);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-text {
  font-size: 13px;
  color: var(--ink-400);
}

/* ---------- 底部免责 ---------- */
.home-foot {
  margin-top: 18px;
  text-align: center;
  font-size: 12px;
  color: var(--ink-400);
}

/* ---------- 窄屏: 快捷入口两列 ---------- */
@media (max-width: 1100px) {
  .quick-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* ---------- 移动端: 横幅紧凑 ---------- */
@media (max-width: 768px) {
  .welcome-card :deep(.el-card__body) {
    padding: 18px 16px;
  }

  .welcome-title {
    font-size: 20px;
    margin-bottom: 6px;
  }

  .welcome-sub {
    font-size: 12.5px;
    line-height: 1.7;
  }
}

@media (max-width: 640px) {
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .welcome-leaf {
    display: none;
  }
}
</style>