<template>
  <div class="analysis-page">
    <!-- 周期切换 -->
    <div class="period-switch">
      <el-radio-group v-model="period" @change="loadReport">
        <el-radio-button :value="7">周分析</el-radio-button>
        <el-radio-button :value="30">月分析</el-radio-button>
      </el-radio-group>
      <span class="period-text" v-if="report">{{ report.period }}（{{ report.recordDays }}天有记录）</span>
    </div>

    <template v-if="report">
      <!-- 营养指标概览 -->
      <el-row :gutter="14" class="metric-row">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric">
              <div class="m-value">{{ report.avgCalorie }}</div>
              <div class="m-label">日均热量(kcal)</div>
              <div class="m-sub">推荐 {{ report.avgRecommendCalorie }} kcal</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric">
              <div class="m-value">{{ report.avgProtein }}</div>
              <div class="m-label">日均蛋白(g)</div>
              <div class="m-sub">碳水 {{ report.avgCarbohydrate }}g / 脂肪 {{ report.avgFat }}g</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric">
              <div class="m-value">{{ report.avgSodium }}</div>
              <div class="m-label">日均钠(mg)</div>
              <div class="m-sub">指南建议 &lt;2000mg</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="metric">
              <div class="m-value">{{ report.avgFiber }}</div>
              <div class="m-label">日均纤维(g)</div>
              <div class="m-sub">指南建议 25-30g</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 供能占比 -->
      <el-card shadow="never" class="ratio-card">
        <template #header>供能结构</template>
        <div class="energy-bars">
          <div class="e-bar">
            <span class="e-name">碳水供能</span>
            <el-progress :percentage="Number(report.carbEnergyRatio || 0)" :stroke-width="14" color="#4A7FB5" />
            <span class="e-val">{{ report.carbEnergyRatio }}%（推荐45-65%）</span>
          </div>
          <div class="e-bar">
            <span class="e-name">脂肪供能</span>
            <el-progress :percentage="Number(report.fatEnergyRatio || 0)" :stroke-width="14" color="#D9A13B" />
            <span class="e-val">{{ report.fatEnergyRatio }}%（推荐20-30%）</span>
          </div>
        </div>
      </el-card>

      <!-- 风险列表 -->
      <el-card shadow="never">
        <template #header>
          <div class="risk-header">
            <span>风险分析报告</span>
            <el-tag v-if="!report.risks.length" type="success">未检出风险</el-tag>
          </div>
        </template>
        <el-alert :title="report.overallComment" type="info" :closable="false" class="overall" />
        <div v-if="report.risks.length" class="risk-list">
          <div v-for="r in report.risks" :key="r.type" class="risk-item" :class="`level-${r.level}`">
            <div class="risk-top">
              <el-tag :type="r.level >= 3 ? 'danger' : 'warning'" effect="dark" size="small">
                {{ r.levelText }}
              </el-tag>
              <span class="risk-name">{{ r.name }}</span>
            </div>
            <div class="risk-desc">{{ r.description }}</div>
            <div class="risk-suggestion">建议: {{ r.suggestion }}</div>
          </div>
        </div>
        <el-empty v-else description="各项指标均符合推荐标准，请继续保持" />
      </el-card>
    </template>
    <el-empty v-else-if="!loading" description="暂无分析数据，请先完成几日饮食打卡" />
    <div v-loading="loading" v-else style="height: 200px"></div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { riskAnalysisApi } from '@/api/health'

const period = ref(7)
const report = ref(null)
const loading = ref(false)

/**
 * 加载风险分析报告
 */
async function loadReport() {
  loading.value = true
  try {
    const res = await riskAnalysisApi(period.value)
    report.value = res.data
  } catch (e) {
    report.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadReport)
</script>

<style scoped>
.period-switch {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.period-text {
  color: #909399;
  font-size: 13px;
}

/* 指标卡 */
.metric-row {
  margin-bottom: 14px;
}

.metric {
  text-align: center;
}

.m-value {
  font-size: 26px;
  font-weight: bold;
  color: var(--data-carb);
}

.m-label {
  font-size: 13px;
  color: #606266;
  margin-top: 2px;
}

.m-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* 供能占比 */
.ratio-card {
  margin-bottom: 14px;
}

.e-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.e-name {
  width: 80px;
  font-size: 13px;
  color: #606266;
  text-align: right;
}

.e-val {
  width: 180px;
  font-size: 12px;
  color: #909399;
}

.risk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overall {
  margin-bottom: 14px;
}

/* 风险项 */
.risk-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.risk-item {
  border: 1px solid #ebeef5;
  border-left: 4px solid var(--data-fat);
  border-radius: 6px;
  padding: 12px 14px;
}

.risk-item.level-3 {
  border-left-color: var(--data-protein);
  background: #fef0f0;
}

.risk-item.level-2 {
  background: #fdf6ec;
}

.risk-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.risk-name {
  font-weight: bold;
  color: #303133;
}

.risk-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}

.risk-suggestion {
  font-size: 13px;
  color: var(--brand-500);
  margin-top: 4px;
}
</style>