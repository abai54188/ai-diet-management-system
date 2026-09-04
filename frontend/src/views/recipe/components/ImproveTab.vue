<template>
  <div class="improve-tab">
    <!-- 选择原菜品与改良方向 -->
    <div class="section">
      <el-form :inline="true" label-width="80px">
        <el-form-item label="原菜品">
          <el-select v-model="selectedName" placeholder="选择已生成的菜品" style="width: 260px" :disabled="!store.generatedDishes.length">
            <el-option v-for="d in store.generatedDishes" :key="d.dishName" :label="`${d.dishName} (${d.calorie}kcal)`" :value="d.dishName" />
          </el-select>
        </el-form-item>
        <el-form-item label="改良方向">
          <el-radio-group v-model="direction">
            <el-radio-button v-for="d in directions" :key="d.value" :value="d.value">{{ d.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" :disabled="!selectedName" @click="handleImprove">
            <el-icon><Refresh /></el-icon>&nbsp;生成改良对比
          </el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="!store.generatedDishes.length" type="info" :closable="false"
        title="请先在「生成食谱」Tab生成菜品，或在卡片上点击「改良此菜品」" />
    </div>

    <!-- 左右对比展示 -->
    <el-empty v-if="!result && !loading" description="选择菜品与改良方向后生成对比" />
    <div v-else v-loading="loading">
      <!-- 差异摘要 -->
      <el-alert v-if="result" :type="calorieDiff < 0 ? 'success' : 'warning'" :closable="false" class="diff-alert">
        <template #title>
          改良方向[{{ directionLabel }}]:
          热量 {{ signed(calorieDiff) }} kcal ({{ caloriePercent }})，
          蛋白质 {{ signed(result.proteinDiff) }}g，
          碳水 {{ signed(result.carbohydrateDiff) }}g，
          脂肪 {{ signed(result.fatDiff) }}g
        </template>
      </el-alert>

      <el-row :gutter="16" v-if="result">
        <!-- 原版 -->
        <el-col :span="12">
          <el-card shadow="hover" class="dish-card">
            <template #header>
              <div class="dish-header">
                <span>{{ result.original.dishName }}</span>
                <el-tag type="info">原版</el-tag>
              </div>
            </template>
            <DishBody :dish="result.original" />
          </el-card>
        </el-col>
        <!-- 改良版 -->
        <el-col :span="12">
          <el-card shadow="hover" class="dish-card improved">
            <template #header>
              <div class="dish-header">
                <span>{{ result.improved.dishName }}</span>
                <el-tag type="success">改良版</el-tag>
              </div>
            </template>
            <DishBody :dish="result.improved" />
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { computed, h, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { improveRecipeApi } from '@/api/aiRecipe'
import { useRecipeStore } from '@/stores/recipe'

const store = useRecipeStore()

// 改良方向选项
const directions = [
  { value: 'LOW_CAL', label: '低卡' },
  { value: 'LOW_FAT', label: '低脂' },
  { value: 'HIGH_PROTEIN', label: '高蛋白' },
  { value: 'LOW_SUGAR', label: '控糖' },
  { value: 'LOW_SALT', label: '低盐' }
]
const direction = ref('LOW_CAL')
const directionLabel = computed(() => directions.find((d) => d.value === direction.value)?.label)

// 选中的原菜品
const selectedName = ref('')
const loading = ref(false)
const result = ref(null)

// 从生成Tab点击「改良此菜品」跳转时自动选中
watch(
  () => store.improveTarget,
  (t) => {
    if (t) {
      selectedName.value = t.dishName
      result.value = null
    }
  },
  { immediate: true }
)

/**
 * 生成改良对比
 */
async function handleImprove() {
  const dish = store.generatedDishes.find((d) => d.dishName === selectedName.value) || store.improveTarget
  if (!dish) {
    ElMessage.warning('未找到菜品，请先生成食谱')
    return
  }
  loading.value = true
  try {
    const res = await improveRecipeApi({
      dishName: dish.dishName,
      ingredients: (dish.ingredients || []).map((i) => ({ foodName: i.foodName, weight: i.weight })),
      steps: dish.steps,
      direction: direction.value
    })
    result.value = res.data
    ElMessage.success('改良方案已生成')
  } finally {
    loading.value = false
  }
}

// 差值展示辅助
const calorieDiff = computed(() => Number(result.value?.calorieDiff || 0))
const caloriePercent = computed(() => {
  const o = Number(result.value?.original?.calorie || 0)
  return o > 0 ? ((calorieDiff.value / o) * 100).toFixed(1) + '%' : '-'
})
function signed(v) {
  const n = Number(v || 0)
  return n > 0 ? `+${n}` : `${n}`
}

/**
 * 菜品卡片内容(内联函数式组件: 营养行+食材表+步骤)
 */
const DishBody = {
  props: { dish: { type: Object, required: true } },
  setup(props) {
    return () =>
      h('div', { class: 'dish-body' }, [
        h('div', { class: 'nutrition-line' },
          `热量 ${props.dish.calorie} kcal · 蛋白 ${props.dish.protein}g · 碳水 ${props.dish.carbohydrate}g · 脂肪 ${props.dish.fat}g`),
        h('div', { class: 'desc-line' }, props.dish.description || ''),
        ...((props.dish.ingredients || []).map((i) =>
          h('div', { class: 'ing-line' }, [
            h('span', {}, `${i.foodName} ${i.weight}g`),
            h('span', {
              class: i.matched ? 'match-ok' : 'match-fail'
            }, i.matched ? `${i.category}` : '未匹配')
          ])
        )),
        ...((props.dish.steps || []).map((s, idx) =>
          h('div', { class: 'step-line' }, `${idx + 1}. ${s}`)
        ))
      ])
  }
}
</script>

<style scoped>
.section {
  margin-bottom: 18px;
}

/* 差异摘要 */
.diff-alert {
  margin-bottom: 16px;
}

/* 改良版卡片描边 */
.dish-card.improved {
  border-color: var(--brand-400);
}

:deep(.dish-body) {
  font-size: 13px;
  color: #606266;
}

:deep(.nutrition-line) {
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

:deep(.desc-line) {
  margin-bottom: 8px;
}

:deep(.ing-line) {
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
  border-bottom: 1px dashed #ebeef5;
}

:deep(.match-ok) {
  color: var(--brand-500);
  font-size: 12px;
}

:deep(.match-fail) {
  color: var(--data-protein);
  font-size: 12px;
}

:deep(.step-line) {
  padding: 3px 0;
  line-height: 1.7;
}

.dish-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
</style>