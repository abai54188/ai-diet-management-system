<template>
  <div class="weight-page">
    <el-row :gutter="16">
      <!-- 左侧: 趋势图 -->
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>体重趋势（近{{ days }}天）</span>
              <el-radio-group v-model="days" size="small" @change="loadTrend">
                <el-radio-button :value="7">周</el-radio-button>
                <el-radio-button :value="30">月</el-radio-button>
                <el-radio-button :value="90">季</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="chartRef" class="chart"></div>
        </el-card>
      </el-col>

      <!-- 右侧: 记录与预测 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>今日称重</template>
          <el-form label-width="70px">
            <el-form-item label="体重(kg)">
              <el-input-number v-model="todayWeight" :min="25" :max="300" :precision="1" style="width: 100%" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">保存记录</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" style="margin-top: 16px" v-if="trend">
          <template #header>达标预测</template>
          <div class="predict-lines">
            <div class="p-line"><span>当前体重</span><b>{{ trend.currentWeight }} kg</b></div>
            <div class="p-line"><span>目标体重</span><b>{{ trend.targetWeight ?? '未设置' }} kg</b></div>
            <div class="p-line"><span>还需变化</span><b>{{ trend.remainingKg ?? '-' }} kg</b></div>
            <div class="p-line"><span>日均变化</span><b>{{ trend.dailyChange }} kg/天</b></div>
            <div class="p-line"><span>热量缺口</span><b>{{ trend.calorieGap ?? '-' }} kcal/天</b></div>
            <div class="p-line highlight">
              <span>预计达标</span>
              <b>{{ trend.forecastDays != null ? trend.forecastDate + `（${trend.forecastDays}天）` : '暂无法预测' }}</b>
            </div>
          </div>
          <el-alert type="info" :closable="false" class="suggestion"
            :title="trend.suggestion || '设置目标体重后可查看达标预测'" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { weightTrendApi, weightSaveApi } from '@/api/health'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const chartRef = ref(null)
const days = ref(30)
const trend = ref(null)
const todayWeight = ref(null)
const saving = ref(false)

// 图表实例
let chart = null

// 页面初始化
onMounted(async () => {
  // 默认今日体重 = 用户当前体重
  todayWeight.value = userStore.userInfo?.weight ? Number(userStore.userInfo.weight) : null
  await loadTrend()
  // 窗口自适应
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
})

/**
 * 加载趋势并渲染图表
 */
async function loadTrend() {
  const res = await weightTrendApi(days.value)
  trend.value = res.data
  await nextTick()
  renderChart(res.data.records || [])
}

/**
 * 渲染ECharts折线图(体重趋势 + 目标线)
 */
function renderChart(records) {
  if (!chartRef.value) return
  chart = chart || echarts.init(chartRef.value)
  const target = trend.value?.targetWeight
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const p = params[0]
        return `${p.axisValue}<br/>体重: <b>${p.data}kg</b>`
      }
    },
    grid: { left: 50, right: 30, top: 40, bottom: 40 },
    xAxis: {
      type: 'category',
      data: records.map((r) => r.date.slice(5)),
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      scale: true,
      name: 'kg',
      nameTextStyle: { color: '#909399' },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#ebeef5' } }
    },
    series: [
      {
        name: '体重',
        type: 'line',
        data: records.map((r) => r.weight),
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { color: '#4A7FB5', width: 3 },
        itemStyle: { color: '#4A7FB5' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(74,127,181,0.28)' },
            { offset: 1, color: 'rgba(74,127,181,0.02)' }
          ])
        },
        markLine: target
          ? {
              symbol: 'none',
              data: [{ yAxis: target, name: '目标' }],
              lineStyle: { color: '#3B8763', type: 'dashed' },
              label: { formatter: `目标 ${target}kg`, color: '#3B8763' }
            }
          : undefined
      }
    ]
  })
}

/**
 * 窗口变化时重设图表尺寸
 */
function resizeChart() {
  chart?.resize()
}

/**
 * 保存今日体重(后端同日覆盖)
 */
async function handleSave() {
  if (!todayWeight.value) return
  saving.value = true
  try {
    await weightSaveApi({
      weight: todayWeight.value,
      recordDate: new Date().toISOString().slice(0, 10)
    })
    ElMessage.success('体重已记录')
    await loadTrend()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart {
  height: 420px;
}

/* 预测行 */
.predict-lines {
  margin-bottom: 12px;
}

.p-line {
  display: flex;
  justify-content: space-between;
  padding: 7px 0;
  border-bottom: 1px dashed #ebeef5;
  font-size: 13px;
  color: #606266;
}

.p-line b {
  color: #303133;
}

.p-line.highlight b {
  color: var(--brand-500);
  font-size: 15px;
}

.suggestion {
  white-space: pre-wrap;
}
</style>