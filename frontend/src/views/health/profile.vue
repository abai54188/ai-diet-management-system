<template>
  <div class="profile-page">
    <el-row :gutter="16">
      <!-- 左侧: 个人信息表单 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>健康档案信息</template>
          <el-form :model="form" label-width="90px" v-loading="loading">
            <el-form-item label="身高(cm)">
              <el-input-number v-model="form.height" :min="80" :max="250" style="width: 100%" />
            </el-form-item>
            <el-form-item label="体重(kg)">
              <el-input-number v-model="form.weight" :min="25" :max="300" :precision="1" style="width: 100%" />
            </el-form-item>
            <el-form-item label="目标体重(kg)">
              <el-input-number v-model="form.targetWeight" :min="25" :max="300" :precision="1" style="width: 100%" />
            </el-form-item>
            <el-form-item label="年龄(岁)">
              <el-input-number v-model="form.age" :min="5" :max="120" style="width: 100%" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="活动量">
              <el-select v-model="form.activityLevel" style="width: 100%">
                <el-option label="久坐(办公室/几乎不运动)" value="SEDENTARY" />
                <el-option label="轻度(每周运动1-3次)" value="LIGHT" />
                <el-option label="中度(每周运动3-5次)" value="MODERATE" />
                <el-option label="高度(每天高强度运动)" value="HIGH" />
              </el-select>
            </el-form-item>
            <el-form-item label="健康目标">
              <el-radio-group v-model="form.healthGoal">
                <el-radio-button value="LOSE">减脂</el-radio-button>
                <el-radio-button value="KEEP">维持</el-radio-button>
                <el-radio-button value="GAIN">增重</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="忌口/过敏">
              <el-input v-model="form.allergy" placeholder="如: 海鲜,花生,麸质" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="calcLoading" @click="handleSave">保存并计算</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧: 计算结果 -->
      <el-col :span="14">
        <el-card shadow="never" v-if="calc">
          <template #header>每日推荐摄入量（Mifflin-St Jeor公式）</template>
          <!-- 核心热量卡片 -->
          <div class="cal-cards">
            <div class="cal-card bmr">
              <div class="value">{{ calc.bmr }}</div>
              <div class="label">基础代谢 BMR</div>
            </div>
            <div class="cal-card tdee">
              <div class="value">{{ calc.tdee }}</div>
              <div class="label">总消耗 TDEE</div>
            </div>
            <div class="cal-card daily">
              <div class="value">{{ calc.dailyCalorie }}</div>
              <div class="label">每日推荐(kcal)</div>
            </div>
          </div>
          <!-- 三大营养素 -->
          <div class="macro-section">
            <div class="macro-title">三大营养素每日目标</div>
            <div class="macro-cards">
              <div class="macro-card protein">
                <div class="name">蛋白质 {{ calc.proteinRatio }}%</div>
                <div class="gram">{{ calc.proteinGram }} g</div>
              </div>
              <div class="macro-card carb">
                <div class="name">碳水 {{ calc.carbRatio }}%</div>
                <div class="gram">{{ calc.carbGram }} g</div>
              </div>
              <div class="macro-card fat">
                <div class="name">脂肪 {{ calc.fatRatio }}%</div>
                <div class="gram">{{ calc.fatGram }} g</div>
              </div>
            </div>
          </div>
          <!-- 三餐比例设置 -->
          <div class="ratio-section">
            <div class="macro-title">三餐热量比例（自定义）</div>
            <div class="ratio-sliders">
              <div class="ratio-item">
                <span>早餐 {{ ratioForm.breakfast }}%</span>
                <el-slider v-model="ratioForm.breakfast" :min="10" :max="60" style="flex: 1; margin: 0 12px" />
              </div>
              <div class="ratio-item">
                <span>午餐 {{ ratioForm.lunch }}%</span>
                <el-slider v-model="ratioForm.lunch" :min="10" :max="60" style="flex: 1; margin: 0 12px" />
              </div>
              <div class="ratio-item">
                <span>晚餐 {{ ratioForm.dinner }}%</span>
                <el-slider v-model="ratioForm.dinner" :min="10" :max="60" style="flex: 1; margin: 0 12px" />
              </div>
            </div>
            <div class="ratio-preview">
              早餐 {{ mealCal(ratioForm.breakfast) }} kcal · 午餐 {{ mealCal(ratioForm.lunch) }} kcal ·
              晚餐 {{ mealCal(ratioForm.dinner) }} kcal
              <el-button type="primary" size="small" style="margin-left: 12px" :loading="ratioLoading"
                :disabled="ratioSum !== 100" @click="saveRatio">
                {{ ratioSum === 100 ? '保存比例' : `比例之和须为100%(当前${ratioSum}%)` }}
              </el-button>
            </div>
          </div>
          <el-alert type="info" :closable="false" class="disclaimer"
            title="以上结果基于Mifflin-St Jeor公式与WHO活动系数计算，仅供饮食参考，不构成医疗建议。" />
        </el-card>
        <el-empty v-else description="填写左侧健康信息后保存，查看每日推荐摄入量" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserInfoApi } from '@/api/auth'
import { updateProfileApi, healthCalcApi, healthProfileApi, setMealRatioApi } from '@/api/health'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 表单(用户基础健康信息)
const form = reactive({
  height: null, weight: null, targetWeight: null, age: null,
  gender: 1, activityLevel: 'LIGHT', healthGoal: 'KEEP', allergy: ''
})
const loading = ref(false)
const calcLoading = ref(false)
const ratioLoading = ref(false)

// 计算结果
const calc = ref(null)

// 三餐比例表单
const ratioForm = reactive({ breakfast: 30, lunch: 40, dinner: 30 })
const ratioSum = computed(() => ratioForm.breakfast + ratioForm.lunch + ratioForm.dinner)

// 页面初始化: 拉取用户信息与档案
onMounted(async () => {
  loading.value = true
  try {
    await userStore.refreshUserInfo()
    const info = userStore.userInfo || {}
    form.height = info.height ? Number(info.height) : null
    form.weight = info.weight ? Number(info.weight) : null
    form.targetWeight = info.targetWeight ? Number(info.targetWeight) : null
    form.age = info.age
    form.gender = info.gender || 1
    form.activityLevel = info.activityLevel || 'LIGHT'
    form.healthGoal = info.healthGoal || 'KEEP'
    form.allergy = info.allergy || ''
    // 加载已有计算结果
    try {
      const res = await healthProfileApi()
      calc.value = res.data
      const r = (res.data.mealRatio || '30,40,30').split(',')
      ratioForm.breakfast = Number(r[0])
      ratioForm.lunch = Number(r[1])
      ratioForm.dinner = Number(r[2])
    } catch (e) { /* 未完善档案时忽略 */ }
  } finally {
    loading.value = false
  }
})

/**
 * 保存信息并重新计算档案
 */
async function handleSave() {
  if (!form.height || !form.weight || !form.age) {
    ElMessage.warning('请完整填写身高、体重、年龄')
    return
  }
  calcLoading.value = true
  try {
    // 1. 保存用户信息
    await updateProfileApi(form)
    await userStore.refreshUserInfo()
    // 2. 计算并保存档案
    const res = await healthCalcApi()
    calc.value = res.data
    ElMessage.success('档案已更新')
  } finally {
    calcLoading.value = false
  }
}

/**
 * 按比例计算餐次热量
 */
function mealCal(percent) {
  if (!calc.value) return '-'
  return Math.round((calc.value.dailyCalorie * percent) / 100)
}

/**
 * 保存自定义三餐比例
 */
async function saveRatio() {
  ratioLoading.value = true
  try {
    const res = await setMealRatioApi({ ...ratioForm })
    calc.value = res.data
    ElMessage.success('三餐比例已保存')
  } finally {
    ratioLoading.value = false
  }
}
</script>

<style scoped>
/* 热量三卡 */
.cal-cards {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.cal-card {
  flex: 1;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  color: #fff;
}

.cal-card.bmr { background: #F1F0FA; color: #4A4494; }
.cal-card.tdee { background: var(--brand-50); color: var(--brand-700); }
.cal-card.daily { background: #FDF1E9; color: #A85A2E; }

.cal-card .value {
  font-size: 30px;
  font-weight: bold;
}

.cal-card .label {
  font-size: 12px;
  opacity: 0.9;
  margin-top: 4px;
}

/* 营养素 */
.macro-title {
  font-weight: bold;
  margin-bottom: 10px;
}

.macro-section { margin-bottom: 20px; }

.macro-cards {
  display: flex;
  gap: 12px;
}

.macro-card {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
  text-align: center;
  color: #fff;
}

.macro-card.protein { background: #FAEDEC; color: #A03F38; }
.macro-card.carb { background: #EBF1F7; color: #35608C; }
.macro-card.fat { background: #FAF2DE; color: #9A6E1E; }

.macro-card .name { font-size: 13px; opacity: 0.95; }
.macro-card .gram { font-size: 22px; font-weight: 500; margin-top: 4px; font-family: var(--font-mono); }

/* 三餐比例 */
.ratio-section { margin-bottom: 16px; }

.ratio-item {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
  font-size: 13px;
  color: #606266;
  width: 90%;
}

.ratio-preview {
  font-size: 13px;
  color: #606266;
  margin-top: 8px;
}

.disclaimer { margin-top: 8px; }
</style>