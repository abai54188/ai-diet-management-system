<template>
  <div class="week-tab">
    <!-- 规划条件 -->
    <div class="section">
      <el-form :inline="true" label-width="70px">
        <el-form-item label="健康目标">
          <el-select v-model="form.healthGoal" style="width: 130px">
            <el-option label="减脂" value="LOSE" />
            <el-option label="维持" value="KEEP" />
            <el-option label="增重" value="GAIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="用餐人数">
          <el-input-number v-model="form.servings" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="忌口">
          <el-input v-model="form.allergy" placeholder="如: 海鲜" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleGenerate">
            <el-icon><Calendar /></el-icon>&nbsp;生成周计划
          </el-button>
          <el-button v-if="store.weekPlan" @click="addWeekToShopping">整周加入购物清单</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 一周日历式展示 -->
    <el-empty v-if="!store.weekPlan && !loading" description="设置条件后点击生成，将为您规划周一到周日三餐" />
    <el-row :gutter="14" v-loading="loading">
      <el-col :span="6" v-for="day in store.weekPlan?.days || []" :key="day.dayLabel" class="day-col">
        <el-card shadow="hover" class="day-card">
          <template #header>
            <div class="day-header">
              <span class="day-label">{{ day.dayLabel }}</span>
              <el-tag type="warning" size="small">{{ day.calorie }} kcal</el-tag>
            </div>
          </template>
          <!-- 三餐列表 -->
          <div v-for="m in mealSlots" :key="m.key" class="meal-block">
            <div class="meal-title">
              <el-tag :type="m.tagType" size="small">{{ m.label }}</el-tag>
              <span class="meal-cal">{{ day[m.key]?.calorie || 0 }} kcal</span>
            </div>
            <div class="meal-name" :title="day[m.key]?.description">{{ day[m.key]?.dishName || '-' }}</div>
            <el-button
              link
              type="primary"
              size="small"
              :loading="replacing[`${day.dayLabel}-${m.key}`]"
              @click="replaceMeal(day, m.key)"
            >
              替换{{ m.label }}
            </el-button>
          </div>
          <!-- 当日营养汇总 -->
          <div class="day-nutrition">
            蛋白 {{ day.protein }}g · 碳水 {{ day.carbohydrate }}g · 脂肪 {{ day.fat }}g
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { weekPlanApi, replaceDishApi } from '@/api/aiRecipe'
import { useRecipeStore } from '@/stores/recipe'

const store = useRecipeStore()

// 三个餐次插槽定义
const mealSlots = [
  { key: 'breakfast', label: '早餐', tagType: 'success' },
  { key: 'lunch', label: '午餐', tagType: 'primary' },
  { key: 'dinner', label: '晚餐', tagType: 'info' }
]

// 规划条件
const form = reactive({ healthGoal: 'LOSE', servings: 2, allergy: '' })
const loading = ref(false)

// 各餐次替换中的加载状态
const replacing = reactive({})

/**
 * 生成一周食谱规划
 */
async function handleGenerate() {
  loading.value = true
  try {
    const res = await weekPlanApi({
      healthGoal: form.healthGoal,
      servings: form.servings,
      allergy: form.allergy || undefined
    })
    store.weekPlan = res.data
    ElMessage.success('周食谱已生成')
  } finally {
    loading.value = false
  }
}

/**
 * 替换单餐菜品: 调用后端获取新菜品(营养本地计算)，前端重算当日汇总
 */
async function replaceMeal(day, mealKey) {
  const key = `${day.dayLabel}-${mealKey}`
  replacing[key] = true
  try {
    const res = await replaceDishApi({
      mealType: mealKey.toUpperCase(),
      dayLabel: day.dayLabel,
      healthGoal: form.healthGoal,
      allergy: form.allergy || undefined,
      servings: form.servings,
      // 排除当日三餐避免替换后重复
      excludeDishNames: [day.breakfast?.dishName, day.lunch?.dishName, day.dinner?.dishName].filter(Boolean)
    })
    day[mealKey] = res.data
    recalcDay(day)
    ElMessage.success(`已替换${day.dayLabel}${mealSlots.find((m) => m.key === mealKey)?.label}`)
  } finally {
    replacing[key] = false
  }
}

/**
 * 重算当日营养(基于后端已计算的各餐数值求和)
 */
function recalcDay(day) {
  const sum = (field) =>
    ['breakfast', 'lunch', 'dinner'].reduce((acc, k) => acc + Number(day[k]?.[field] || 0), 0)
  day.calorie = sum('calorie').toFixed(1)
  day.protein = sum('protein').toFixed(1)
  day.carbohydrate = sum('carbohydrate').toFixed(1)
  day.fat = sum('fat').toFixed(1)
}

/**
 * 整周菜品加入购物清单
 */
function addWeekToShopping() {
  const dishes = store.weekPlan.days.flatMap((d) => [d.breakfast, d.lunch, d.dinner]).filter(Boolean)
  const added = store.addDishesToShopping(dishes)
  ElMessage.success(added ? `已将 ${added} 道菜品加入购物清单` : '菜品已在购物清单中')
}
</script>

<style scoped>
.section {
  margin-bottom: 16px;
}

/* 日卡片 */
.day-col {
  margin-bottom: 14px;
}

.day-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.day-label {
  font-weight: bold;
}

/* 餐次块 */
.meal-block {
  border-bottom: 1px dashed #ebeef5;
  padding: 8px 0;
}

.meal-block:last-of-type {
  border-bottom: none;
}

.meal-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.meal-cal {
  font-size: 12px;
  color: var(--data-cal);
}

.meal-name {
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 当日营养汇总 */
.day-nutrition {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}
</style>