<template>
  <div class="dashboard-page">
    <!-- 周期切换 -->
    <div class="dash-header">
      <el-radio-group v-model="days" @change="loadAll">
        <el-radio-button :value="7">近一周</el-radio-button>
        <el-radio-button :value="30">近一月</el-radio-button>
      </el-radio-group>
      <!-- 达标率卡片 -->
      <div class="achieve-card" v-if="achievement">
        <div class="achieve-info">
          <div class="a-label">热量达标率</div>
          <div class="a-value">{{ achievement.rate }}%</div>
          <div class="a-sub">{{ achievement.hitDays }}/{{ achievement.recordDays }}天达标 (推荐{{ achievement.recommend }}kcal ±10%)</div>
        </div>
        <el-progress type="dashboard" :percentage="Number(achievement.rate) || 0" :width="90"
          :color="achieveColor" />
      </div>
    </div>

    <!-- 图表网格 -->
    <el-row :gutter="16">
      <!-- 热量趋势折线图 -->
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>热量摄入趋势（kcal）</template>
          <div ref="trendChartRef" class="chart-lg" />
        </el-card>
      </el-col>
      <!-- 营养素饼图 + 食材分类柱状图 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>三大营养素供能占比</template>
          <div ref="macroChartRef" class="chart-md" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>食材分类摄入统计</template>
          <div ref="categoryChartRef" class="chart-md" />
        </el-card>
      </el-col>
      <!-- 分时段柱状图 -->
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>分时段摄入统计（{{ days }}天累计）</template>
          <div ref="mealChartRef" class="chart-md" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import {
  calorieTrendApi, macroRatioApi, foodCategoryApi,
  mealPeriodApi, achievementApi
} from '@/api/community'

const days = ref(7)

// 图表DOM引用
const trendChartRef = ref(null)
const macroChartRef = ref(null)
const categoryChartRef = ref(null)
const mealChartRef = ref(null)

// 图表实例
let charts = []
const achievement = ref(null)
const achieveColor = [
  [0.4, '#C95850'], [0.7, '#D9A13B'], [1, '#3B8763']
]

onMounted(async () => {
  await loadAll()
  // 窗口缩放自适应(所有图表)
  window.addEventListener('resize', resizeAll)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  charts.forEach((c) => c.dispose())
})

/**
 * 加载全部统计数据
 */
async function loadAll() {
  const [trend, macro, category, meal, achieve] = await Promise.all([
    calorieTrendApi(days.value),
    macroRatioApi(days.value),
    foodCategoryApi(days.value),
    mealPeriodApi(days.value),
    achievementApi(days.value).catch(() => null)
  ])
  achievement.value = achieve?.data || null
  await nextTick()
  renderTrend(trend.data || [])
  renderMacro(macro.data || {})
  renderCategory(category.data || [])
  renderMeal(meal.data || [])
}

/**
 * 热量趋势折线图
 */
function renderTrend(data) {
  const chart = initChart(trendChartRef)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 30, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: data.map((d) => d.date.slice(5)), axisLabel: { color: '#909399' } },
    yAxis: { type: 'value', name: 'kcal', axisLabel: { color: '#909399' }, splitLine: { lineStyle: { color: '#ebeef5' } } },
    series: [{
      type: 'line',
      data: data.map((d) => d.calorie),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#D97742', width: 3 },
      itemStyle: { color: '#D97742' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(217,119,66,0.25)' },
          { offset: 1, color: 'rgba(217,119,66,0.02)' }
        ])
      },
      markLine: achievement.value ? {
        symbol: 'none',
        data: [{ yAxis: Number(achievement.value.recommend) }],
        lineStyle: { color: '#3B8763', type: 'dashed' },
        label: { formatter: '推荐线', color: '#3B8763' }
      } : undefined
    }]
  })
}

/**
 * 营养素供能饼图
 */
function renderMacro(data) {
  const chart = initChart(macroChartRef)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}g ({d}%)' },
    legend: { bottom: 10 },
    series: [{
      type: 'pie',
      radius: ['40%', '68%'],
      center: ['50%', '44%'],
      label: { formatter: '{b}\n{d}%' },
      data: [
        { value: data.protein || 0, name: '蛋白质', itemStyle: { color: '#C95850' } },
        { value: data.carbohydrate || 0, name: '碳水', itemStyle: { color: '#4A7FB5' } },
        { value: data.fat || 0, name: '脂肪', itemStyle: { color: '#D9A13B' } }
      ]
    }]
  })
}

/**
 * 食材分类柱状图(横向)
 */
function renderCategory(data) {
  const chart = initChart(categoryChartRef)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c}次' },
    grid: { left: 90, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value', axisLabel: { color: '#909399' }, splitLine: { lineStyle: { color: '#ebeef5' } } },
    yAxis: { type: 'category', data: data.map((d) => d.category), axisLabel: { color: '#606266' } },
    series: [{
      type: 'bar',
      data: data.map((d) => d.count),
      barWidth: 14,
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#5A8F6E' },
          { offset: 1, color: '#3B8763' }
        ])
      }
    }]
  })
}

/**
 * 分时段柱状图
 */
function renderMeal(data) {
  const chart = initChart(mealChartRef)
  chart.setOption({
    tooltip: { trigger: 'axis', formatter: (p) => `${p[0].name}<br/>热量: ${p[0].value} kcal<br/>记录: ${p[1].value}次` },
    legend: { top: 0 },
    grid: { left: 60, right: 30, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: data.map((d) => d.label), axisLabel: { color: '#606266' } },
    yAxis: [
      { type: 'value', name: 'kcal', axisLabel: { color: '#909399' }, splitLine: { lineStyle: { color: '#ebeef5' } } },
      { type: 'value', name: '次数', axisLabel: { color: '#909399' }, splitLine: { show: false } }
    ],
    series: [
      {
        name: '热量',
        type: 'bar',
        data: data.map((d) => d.calorie),
        barWidth: 36,
        itemStyle: { borderRadius: [6, 6, 0, 0], color: '#3B8763' }
      },
      {
        name: '记录数',
        type: 'line',
        yAxisIndex: 1,
        data: data.map((d) => d.count),
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#D9A13B', width: 2 },
        itemStyle: { color: '#D9A13B' }
      }
    ]
  })
}

/**
 * 初始化或复用图表实例
 */
function initChart(domRef) {
  const dom = domRef.value
  // 复用已有实例(切换周期时重绘)
  const exist = charts.find((c) => c.getDom() === dom)
  if (exist) {
    return exist
  }
  const chart = echarts.init(dom)
  charts.push(chart)
  return chart
}

/**
 * 全部图表自适应
 */
function resizeAll() {
  charts.forEach((c) => c.resize())
}
</script>

<style scoped>
.dash-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}

/* 达标率卡片 */
.achieve-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: var(--brand-50);
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  padding: 10px 20px;
}

.a-label {
  font-size: 13px;
  color: #606266;
}

.a-value {
  font-size: 30px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.a-sub {
  font-size: 12px;
  color: #909399;
}

/* 图表尺寸 */
.chart-lg {
  height: 320px;
}

.chart-md {
  height: 300px;
}
</style>