<template>
  <div>
    <!-- 指标卡 -->
    <el-row :gutter="14" class="metric-row">
      <el-col :span="4" v-for="m in metrics" :key="m.label">
        <el-card shadow="hover">
          <div class="metric" :style="{ background: m.bg }">
            <div class="m-value">{{ m.value }}</div>
            <div class="m-label">{{ m.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 双趋势图 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>近7日新增用户</template>
          <div ref="userChartRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>近7日饮食打卡量</template>
          <div ref="dietChartRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { adminOverviewApi } from '@/api/community'

const overview = ref(null)
const userChartRef = ref(null)
const dietChartRef = ref(null)
let userChart = null
let dietChart = null

// 指标卡定义
const metrics = ref([])

onMounted(async () => {
  const res = await adminOverviewApi()
  overview.value = res.data
  metrics.value = [
    { label: '注册用户', value: res.data.userCount, bg: '#EBF1F7' },
    { label: '社区动态', value: res.data.postCount, bg: '#E3F0E8' },
    { label: '评论数', value: res.data.commentCount, bg: '#FAF2DE' },
    { label: '食材库', value: res.data.foodCount, bg: '#F2F7F3' },
    { label: '饮食打卡', value: res.data.dietRecordCount, bg: '#FDF1E9' },
    { label: '待处理举报', value: res.data.pendingReports, bg: '#FAEDEC' }
  ]
  await nextTick()
  renderCharts(res.data)
  window.addEventListener('resize', resizeAll)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  userChart?.dispose()
  dietChart?.dispose()
})

/**
 * 渲染双趋势图
 */
function renderCharts(data) {
  const dates = (data.newUserTrend || []).map((d) => d.date.slice(5))
  userChart = echarts.init(userChartRef.value)
  userChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { color: '#909399' } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { color: '#909399' }, splitLine: { lineStyle: { color: '#ebeef5' } } },
    series: [{
      type: 'bar', data: (data.newUserTrend || []).map((d) => d.count), barWidth: 22,
      itemStyle: { borderRadius: [5, 5, 0, 0], color: '#3B8763' }
    }]
  })
  dietChart = echarts.init(dietChartRef.value)
  dietChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { color: '#909399' } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { color: '#909399' }, splitLine: { lineStyle: { color: '#ebeef5' } } },
    series: [{
      type: 'line', data: (data.dietTrend || []).map((d) => d.count), smooth: true,
      symbol: 'circle', symbolSize: 7,
      lineStyle: { color: '#3B8763', width: 3 }, itemStyle: { color: '#3B8763' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(59,135,99,0.25)' },
          { offset: 1, color: 'rgba(59,135,99,0.02)' }
        ])
      }
    }]
  })
}

/**
 * 图表自适应
 */
function resizeAll() {
  userChart?.resize()
  dietChart?.resize()
}
</script>

<style scoped>
.metric-row {
  margin-bottom: 16px;
}

.metric {
  text-align: center;
  border-radius: 8px;
  padding: 16px 6px;
}

.m-value {
  font-size: 26px;
  font-weight: bold;
  color: #303133;
}

.m-label {
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
}

.chart {
  height: 300px;
}
</style>