<template>
  <div class="diet-page">
    <el-row :gutter="16">
      <!-- 左侧: 添加打卡 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <div class="add-header">
              <span>添加饮食记录</span>
              <el-tag type="success" size="small" effect="plain">AI 智能分析</el-tag>
            </div>
          </template>

          <!-- 拍照打卡(手机端主入口) -->
          <div class="photo-zone" :class="{ uploading: photoAnalyzing }"
            v-loading="photoAnalyzing" element-loading-text="AI 识别中，请稍候…"
            @click="triggerPhoto">
            <el-icon :size="26"><Camera /></el-icon>
            <div class="photo-title">拍照打卡</div>
            <div class="photo-desc">拍下食物，AI 自动识别并计算热量</div>
            <input ref="photoInputRef" type="file" accept="image/*" capture="environment"
              hidden @change="handlePhotoChange" />
          </div>

          <div class="divider"><span>或输入菜名</span></div>

          <!-- AI智能识别(唯一模式) -->
          <div class="ai-mode">
            <el-alert type="info" :closable="false" class="ai-tip"
              title="输入任意食物或菜品名称(如: 宫保鸡丁、牛肉面)，AI自动解析食材并按营养库计算热量" />
            <el-form label-width="70px">
              <el-form-item label="选择时段">
                <el-radio-group v-model="addForm.mealType">
                  <el-radio-button value="BREAKFAST">早餐</el-radio-button>
                  <el-radio-button value="LUNCH">午餐</el-radio-button>
                  <el-radio-button value="DINNER">晚餐</el-radio-button>
                  <el-radio-button value="SNACK">加餐</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="食物名称">
                <el-input v-model="aiForm.foodName" placeholder="如: 番茄炒蛋、皮蛋瘦肉粥、麻辣香锅"
                  clearable @keyup.enter="handleAiAnalyze">
                  <template #append>
                    <el-button :loading="analyzing" @click="handleAiAnalyze">AI 解析</el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="吃完重量">
                <el-input-number v-model="aiForm.totalWeight" :min="1" :max="5000" :step="10" style="width: 100%" />
                <div class="weight-tip">这道菜实际吃了多少克，AI 按比例分配到各食材</div>
              </el-form-item>
            </el-form>

            <!-- 解析结果 -->
            <div v-if="aiResult" class="ai-result" v-loading="analyzing">
              <div class="ai-summary">
                <b>{{ displayResult.foodName }}</b>
                共 {{ displayResult.ingredients.length }} 项食材
                <span class="ai-cal">合计 {{ displayResult.totalCalorie }} kcal</span>
                <span class="ai-macro">P{{ displayResult.totalProtein }}g / C{{ displayResult.totalCarbohydrate }}g / F{{ displayResult.totalFat }}g</span>
                <el-tag v-if="aiResult.unmatchedCount > 0" type="warning" size="small">
                  {{ aiResult.unmatchedCount }}项未匹配
                </el-tag>
              </div>
              <el-table :data="displayResult.ingredients" size="small" max-height="240">
                <el-table-column prop="foodName" label="食材" min-width="100" />
                <el-table-column label="重量" width="75" align="center">
                  <template #default="{ row }">{{ row.weight }}g</template>
                </el-table-column>
                <el-table-column prop="calorie" label="热量" width="80" align="center">
                  <template #default="{ row }">{{ row.matched ? row.calorie : '-' }}</template>
                </el-table-column>
                <el-table-column label="匹配" width="70" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.matched ? 'success' : 'info'" size="small">
                      {{ row.matched ? row.category : '未匹配' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-button type="primary" style="width: 100%; margin-top: 10px" :loading="adding"
                @click="handleAiAdd">
                确认打卡（{{ displayResult.totalCalorie }} kcal）
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 当日汇总卡片 -->
        <el-card shadow="never" style="margin-top: 16px" v-if="summary">
          <template #header>当日营养汇总</template>
          <div class="today-cards">
            <div class="t-card">
              <div class="t-value">{{ summary.totalCalorie }}</div>
              <div class="t-label">实际(kcal)</div>
            </div>
            <div class="t-card">
              <div class="t-value">{{ summary.recommendCalorie || '-' }}</div>
              <div class="t-label">推荐(kcal)</div>
            </div>
            <div class="t-card">
              <div class="t-value" :class="diffClass">{{ summary.calorieDiff ?? '-' }}</div>
              <div class="t-label">差值(kcal)</div>
            </div>
          </div>
          <div class="macro-lines">
            <div>蛋白质 {{ summary.totalProtein }}g（{{ macroRate(summary.proteinDiff) }}）</div>
            <div>碳水 {{ summary.totalCarbohydrate }}g（{{ macroRate(summary.carbDiff) }}）</div>
            <div>脂肪 {{ summary.totalFat }}g（{{ macroRate(summary.fatDiff) }}）</div>
            <div>钠 {{ summary.totalSodium }}mg · 纤维 {{ summary.totalFiber }}g</div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧: 进度条与记录列表 -->
      <el-col :span="16">
        <!-- 摄入进度 -->
        <el-card shadow="never" v-if="summary">
          <template #header>
            <div class="progress-header">
              <span>今日摄入进度</span>
              <span class="date-text">{{ today }}</span>
            </div>
          </template>
          <!-- 总热量进度 -->
          <div class="progress-item">
            <div class="p-label">
              <span>总热量 {{ progressPercent }}%</span>
              <span>{{ summary.totalCalorie }} / {{ summary.recommendCalorie || '-' }} kcal</span>
            </div>
            <el-progress :percentage="progressPercent" :color="progressColor" :stroke-width="16" />
          </div>
          <!-- 分餐进度 -->
          <div class="progress-item" v-for="m in summary.meals" :key="m.mealType">
            <div class="p-label">
              <span>{{ m.mealLabel }} {{ mealPercent(m) }}%</span>
              <span>{{ m.calorie }} / {{ m.targetCalorie || '不限' }} kcal</span>
            </div>
            <el-progress
              :percentage="mealPercent(m)"
              :color="mealPercent(m) > 130 ? '#C95850' : '#3B8763'"
              :stroke-width="12"
            />
          </div>
        </el-card>

        <!-- 当日记录表 -->
        <el-card shadow="never" style="margin-top: 16px">
          <template #header>
            <div class="progress-header">
              <span>今日打卡记录（{{ records.length }}条）</span>
              <el-button type="primary" link @click="refresh">刷新</el-button>
            </div>
          </template>
          <el-table :data="records" size="small" v-loading="recordLoading" empty-text="今日暂无记录">
            <el-table-column prop="foodName" label="食材" min-width="120" />
            <el-table-column label="重量" width="90" align="center">
              <template #default="{ row }">{{ row.weight }}g</template>
            </el-table-column>
            <el-table-column :prop="'calorie'" label="热量(kcal)" width="105" align="center" />
            <el-table-column label="蛋白/碳水/脂肪" width="150" align="center">
              <template #default="{ row }">
                {{ row.protein ?? '-' }}/{{ row.carbohydrate ?? '-' }}/{{ row.fat ?? '-' }} g
              </template>
            </el-table-column>
            <el-table-column label="时段" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="mealTagType(row.mealType)" size="small">{{ mealLabel(row.mealType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ row }">
                <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- AI建议 -->
        <el-card shadow="never" style="margin-top: 16px" v-if="advice">
          <template #header>
            <div class="progress-header">
              <span>AI饮食建议</span>
              <el-tag type="success" size="small">AI 大模型生成</el-tag>
            </div>
          </template>
          <div class="advice-text">{{ advice }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dailySummaryApi, dailyAdviceApi, dietSaveApi, dietDeleteApi, dietPageApi, aiAnalyzeFoodApi, aiPhotoAnalyzeApi } from '@/api/health'

const today = new Date().toISOString().slice(0, 10)

// 添加打卡表单(仅保留用餐时段字段)
const addForm = reactive({ mealType: 'BREAKFAST' })
const adding = ref(false)

// AI识别表单与结果
const aiForm = reactive({ foodName: '', totalWeight: 300 })
const analyzing = ref(false)
const aiResult = ref(null)

// 拍照识别: 基准重量与营养(AI识别结果), 用户调整重量时按比例换算
const photoAnalyzing = ref(false)
const photoInputRef = ref(null)
const photoBase = ref(null)

/**
 * 触发拍照/选图(input被隐藏, 点击上传区代为触发)
 */
function triggerPhoto() {
  if (photoAnalyzing.value || analyzing.value) return
  photoInputRef.value?.click()
}

/**
 * 选择照片后: 压缩 -> 上传识别 -> 回填菜名与基准重量
 */
async function handlePhotoChange(e) {
  const file = e.target.files?.[0]
  e.target.value = '' // 允许连续重选同一张照片
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片过大，请重新拍摄')
    return
  }
  photoAnalyzing.value = true
  try {
    const blob = await compressImage(file, 1280, 0.82)
    const fd = new FormData()
    fd.append('image', blob, 'photo.jpg')
    const res = await aiPhotoAnalyzeApi(fd)
    aiResult.value = res.data
    const estWeight = res.data.ingredients.reduce((s, i) => s + (Number(i.weight) || 0), 0)
    photoBase.value = {
      weight: estWeight > 0 ? estWeight : 300,
      calorie: Number(res.data.totalCalorie) || 0,
      protein: Number(res.data.totalProtein) || 0,
      carbohydrate: Number(res.data.totalCarbohydrate) || 0,
      fat: Number(res.data.totalFat) || 0
    }
    aiForm.foodName = res.data.foodName
    aiForm.totalWeight = Math.round(photoBase.value.weight)
    ElMessage.success(`识别为「${res.data.foodName}」，可调整重量后打卡`)
  } finally {
    photoAnalyzing.value = false
  }
}

/**
 * canvas 压缩图片: 限制长边尺寸并转JPEG(手机原图2-5MB, 不压缩上传与识别都慢)
 */
function compressImage(file, maxEdge, quality) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => {
      URL.revokeObjectURL(url)
      let { width, height } = img
      if (width >= height && width > maxEdge) {
        height = Math.round((height * maxEdge) / width)
        width = maxEdge
      } else if (height > width && height > maxEdge) {
        width = Math.round((width * maxEdge) / height)
        height = maxEdge
      }
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      canvas.getContext('2d').drawImage(img, 0, 0, width, height)
      canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('压缩失败'))), 'image/jpeg', quality)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片读取失败'))
    }
    img.src = url
  })
}

/**
 * 展示/保存用的解析结果:
 * 拍照模式下用户调整重量时, 营养按 新重量/基准重量 等比换算;
 * 文本模式(无photoBase)直接返回原结果, 与原逻辑一致
 */
const displayResult = computed(() => {
  const r = aiResult.value
  if (!r || !photoBase.value || !aiForm.totalWeight || aiForm.totalWeight <= 0) return r
  const factor = aiForm.totalWeight / photoBase.value.weight
  if (!isFinite(factor) || factor <= 0) return r
  return {
    ...r,
    totalCalorie: round1(r.totalCalorie * factor),
    totalProtein: round1(r.totalProtein * factor),
    totalCarbohydrate: round1(r.totalCarbohydrate * factor),
    totalFat: round1(r.totalFat * factor)
  }
})

/** 保留1位小数 */
function round1(v) {
  return Math.round((Number(v) || 0) * 10) / 10
}

/**
 * AI解析食物: 输入菜名+吃完重量, 后端AI解析食材组成按比例分配重量并本地计算营养
 */
async function handleAiAnalyze() {
  const name = aiForm.foodName.trim()
  if (!name) {
    ElMessage.warning('请输入食物或菜品名称')
    return
  }
  if (!aiForm.totalWeight || aiForm.totalWeight <= 0) {
    ElMessage.warning('请输入吃完的重量(g)')
    return
  }
  analyzing.value = true
  aiResult.value = null
  photoBase.value = null // 文本模式: 清除拍照基准, 走后端已按重量缩放的结果
  try {
    const res = await aiAnalyzeFoodApi({ foodName: name, totalWeight: aiForm.totalWeight })
    aiResult.value = res.data
    ElMessage.success(`已解析出 ${res.data.ingredients.length} 项食材`)
  } finally {
    analyzing.value = false
  }
}

/**
 * AI解析结果确认打卡: 仅打卡一条"菜名记录"(用户输入的菜名+总重量+总热量), 不逐条落食材
 */
async function handleAiAdd() {
  const r = displayResult.value
  if (!r || !r.totalCalorie) {
    ElMessage.warning('请先进行 AI 解析')
    return
  }
  adding.value = true
  try {
    // 整道菜作为一条记录落库: 菜名=用户输入, 重量=实际吃完克数, 热量与宏量营养=AI解析结果
    await dietSaveApi({
      foodName: r.foodName,
      weight: aiForm.totalWeight,
      calorie: r.totalCalorie,
      protein: r.totalProtein,
      carbohydrate: r.totalCarbohydrate,
      fat: r.totalFat,
      mealType: addForm.mealType,
      recordDate: today
    })
    const skipped = r.ingredients.filter((i) => !i.matched).length
    ElMessage.success(`已打卡「${r.foodName}」${aiForm.totalWeight}g${skipped ? `（${skipped} 项食材未匹配已从热量中剔除）` : ''}`)
    aiForm.foodName = ''
    aiResult.value = null
    photoBase.value = null
    await refresh()
  } finally {
    adding.value = false
  }
}

// 当日数据
const summary = ref(null)
const advice = ref('')
const records = ref([])
const recordLoading = ref(false)

/**
 * 删除记录
 */
async function handleDelete(row) {
  await dietDeleteApi(row.id)
  ElMessage.success('已删除')
  await refresh()
}

/**
 * 刷新汇总/记录/建议
 */
async function refresh() {
  recordLoading.value = true
  try {
    const [sum, rec, adv] = await Promise.all([
      dailySummaryApi(today),
      dietPageApi({ current: 1, size: 50, startDate: today, endDate: today }),
      dailyAdviceApi(today)
    ])
    summary.value = sum.data
    records.value = rec.data.records
    advice.value = adv.data.advice
  } finally {
    recordLoading.value = false
  }
}

// 总热量进度
const progressPercent = computed(() => {
  if (!summary.value || !summary.value.recommendCalorie) return 0
  return Math.min(150, Math.round((summary.value.totalCalorie / summary.value.recommendCalorie) * 100))
})
const progressColor = computed(() =>
  progressPercent.value > 110 ? '#C95850' : progressPercent.value > 90 ? '#3B8763' : '#409eff'
)

// 分餐进度
function mealPercent(m) {
  if (!m.targetCalorie) return m.calorie > 0 ? 100 : 0
  return Math.min(150, Math.round((m.calorie / m.targetCalorie) * 100))
}

// 差值颜色
const diffClass = computed(() => {
  const d = summary.value?.calorieDiff
  if (d == null) return ''
  return d > 0 ? 'over' : d < 0 ? 'under' : ''
})

// 营养素达标描述
function macroRate(diff) {
  if (diff == null) return '未设置目标'
  if (diff > 0) return `超出目标${diff}g`
  return `距目标差${Math.abs(diff)}g`
}

// 时段标签
function mealLabel(type) {
  return { BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐' }[type] || type
}
function mealTagType(type) {
  return { BREAKFAST: 'success', LUNCH: 'primary', DINNER: 'info', SNACK: 'warning' }[type] || 'info'
}

onMounted(refresh)
</script>

<style scoped>
.suggest-item {
  display: flex;
  justify-content: space-between;
}

.suggest-cal {
  color: #909399;
  font-size: 12px;
}

/* 添加卡片头部 */
.add-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* AI识别模式 */
.ai-tip {
  margin-bottom: 12px;
}

/* 拍照打卡入口 */
.photo-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 20px 12px;
  border: 1.5px dashed var(--brand-300);
  border-radius: var(--radius-md);
  background: var(--brand-50);
  color: var(--brand-700);
  cursor: pointer;
  text-align: center;
  transition: border-color var(--dur-fast) ease, background-color var(--dur-fast) ease;
}

.photo-zone:hover {
  border-color: var(--brand-500);
  background: var(--brand-100);
}

.photo-zone.uploading {
  cursor: wait;
}

.photo-title {
  font-size: 14px;
  font-weight: 600;
  margin-top: 2px;
}

.photo-desc {
  font-size: 12px;
  color: var(--ink-400);
}

/* 文本/拍照入口分隔线 */
.divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 14px 0;
  color: var(--ink-400);
  font-size: 12px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--line-1);
}

.ai-result {
  margin-top: 6px;
}

.ai-summary {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-cal {
  color: var(--data-cal);
  font-weight: bold;
  font-size: 15px;
}

.ai-macro {
  color: #909399;
  font-size: 12px;
}

.preview-cal {
  color: var(--data-cal);
  font-weight: bold;
  font-size: 16px;
  font-family: var(--font-mono);
}

/* 汇总小卡 */
.today-cards {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.t-card {
  flex: 1;
  text-align: center;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 10px 4px;
}

.t-value {
  font-size: 18px;
  font-family: var(--font-mono);
  font-weight: bold;
  color: #303133;
}

.t-value.over { color: var(--data-protein); }
.t-value.under { color: var(--brand-500); }

.t-label {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.macro-lines {
  font-size: 13px;
  color: #606266;
  line-height: 2;
}

/* 进度条 */
.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.date-text {
  color: #909399;
  font-size: 13px;
}

.progress-item {
  margin-bottom: 14px;
}

.p-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}

.advice-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.9;
  background: var(--brand-50);
  padding: 12px;
  border-radius: 6px;
}
.weight-tip {
  font-size: 11px;
  color: var(--ink-400);
  margin-top: 4px;
  line-height: 1.5;
}

</style>