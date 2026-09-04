<template>
  <div class="generate-tab">
    <!-- 食材输入区 -->
    <div class="section">
      <div class="section-title">食材输入</div>
      <div class="ingredient-row">
        <el-autocomplete
          v-model="keyword"
          :fetch-suggestions="querySearch"
          placeholder="输入食材名称(如: 鸡胸肉)，选择或回车添加"
          clearable
          style="width: 300px"
          @select="handleSuggestSelect"
          @keyup.enter="addRawIngredient"
        />
        <el-button type="primary" @click="addRawIngredient">添加食材</el-button>
        <div class="ingredient-tags">
          <el-tag
            v-for="tag in ingredientTags"
            :key="tag"
            closable
            :type="tagType(tag)"
            class="ing-tag"
            @close="removeTag(tag)"
          >
            {{ tag }}
          </el-tag>
          <span v-if="!ingredientTags.length" class="hint">至少添加一种食材</span>
        </div>
      </div>
    </div>

    <!-- 条件筛选区 -->
    <div class="section">
      <div class="section-title">生成条件</div>
      <el-form :inline="true" label-width="70px">
        <el-form-item label="忌口">
          <el-input v-model="conditions.allergy" placeholder="如: 海鲜,花生" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="口味">
          <el-select v-model="conditions.taste" style="width: 130px">
            <el-option v-for="t in tasteOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜系">
          <el-select v-model="conditions.cuisine" style="width: 130px">
            <el-option v-for="c in cuisineOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="健康目标">
          <el-select v-model="conditions.healthGoal" style="width: 130px">
            <el-option label="减脂" value="LOSE" />
            <el-option label="维持" value="KEEP" />
            <el-option label="增重" value="GAIN" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" :disabled="!ingredientTags.length" @click="handleGenerate">
            <el-icon><MagicStick /></el-icon>&nbsp;生成食谱
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 生成的食谱卡片 -->
    <el-empty v-if="!store.generatedDishes.length && !loading" description="添加食材并点击生成，AI将为您设计3-5套中式菜品" />
    <el-row :gutter="16" v-loading="loading">
      <el-col :span="12" v-for="dish in store.generatedDishes" :key="dish.dishName" class="dish-col">
        <el-card shadow="hover" class="dish-card">
          <template #header>
            <div class="dish-header">
              <span class="dish-name">{{ dish.dishName }}</span>
              <el-tag type="warning" effect="dark">{{ dish.calorie }} kcal</el-tag>
            </div>
          </template>
          <!-- 简介与元信息 -->
          <div class="dish-desc">{{ dish.description }}</div>
          <div class="dish-meta">
            <span>难度:
              <el-rate :model-value="dish.difficulty" disabled :max="3" size="small" />
            </span>
            <span>耗时: {{ dish.cookingTime }}分钟</span>
            <span>蛋白 {{ dish.protein }}g / 碳水 {{ dish.carbohydrate }}g / 脂肪 {{ dish.fat }}g</span>
          </div>
          <!-- 食材列表 -->
          <el-table :data="dish.ingredients" size="small" class="ing-table">
            <el-table-column prop="foodName" label="食材" min-width="90" />
            <el-table-column label="重量" width="80" align="center">
              <template #default="{ row }">{{ row.weight }}g</template>
            </el-table-column>
            <el-table-column label="营养库匹配" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.matched" type="success" size="small">{{ row.category }}</el-tag>
                <el-tag v-else type="danger" size="small">未匹配</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <!-- 步骤 -->
          <ol class="steps">
            <li v-for="(s, i) in dish.steps" :key="i">{{ s }}</li>
          </ol>
          <!-- 操作按钮 -->
          <div class="dish-actions">
            <el-button :type="inShopping(dish) ? 'success' : 'default'" size="small" @click="toggleShopping(dish)">
              {{ inShopping(dish) ? '已加入购物清单' : '加入购物清单' }}
            </el-button>
            <el-button type="warning" size="small" @click="goImprove(dish)">改良此菜品</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { foodSuggestApi } from '@/api/food'
import { generateRecipeApi } from '@/api/aiRecipe'
import { useRecipeStore } from '@/stores/recipe'

const emit = defineEmits(['go-improve'])
const store = useRecipeStore()

// ============ 食材标签输入 ============
const keyword = ref('')
const ingredientTags = ref(['鸡胸肉', '西兰花'])

// 口味与菜系选项
const tasteOptions = ['不限', '清淡', '咸鲜', '香辣', '酸甜', '麻辣']
const cuisineOptions = ['不限', '家常菜', '川菜', '粤菜', '湘菜', '鲁菜', '江浙菜', '东北菜', '西北菜']

/**
 * 食材输入联想(复用营养库suggest接口)
 */
let timer = null
function querySearch(kw, cb) {
  clearTimeout(timer)
  if (!kw || !kw.trim()) {
    cb([])
    return
  }
  timer = setTimeout(async () => {
    const res = await foodSuggestApi(kw.trim())
    cb(res.data || [])
  }, 300)
}

/**
 * 选中联想项添加标签
 */
function handleSuggestSelect(item) {
  addTag(item.foodName)
  keyword.value = ''
}

/**
 * 回车直接添加输入框文本
 */
function addRawIngredient() {
  if (keyword.value.trim()) {
    addTag(keyword.value.trim())
    keyword.value = ''
  }
}

/**
 * 添加食材标签(去重)
 */
function addTag(name) {
  if (!ingredientTags.value.includes(name)) {
    ingredientTags.value.push(name)
  }
}

/**
 * 删除食材标签
 */
function removeTag(tag) {
  ingredientTags.value = ingredientTags.value.filter((t) => t !== tag)
}

/**
 * 标签颜色(前两个主料高亮)
 */
function tagType(tag) {
  const idx = ingredientTags.value.indexOf(tag)
  return idx === 0 ? 'danger' : idx === 1 ? 'warning' : 'info'
}

// ============ 条件与生成 ============
const conditions = reactive({
  allergy: '',
  taste: '清淡',
  cuisine: '家常菜',
  healthGoal: 'KEEP'
})
const loading = ref(false)

/**
 * 调用后端生成食谱(AI仅输出食材+重量，营养由本地库计算)
 */
async function handleGenerate() {
  if (!ingredientTags.value.length) {
    ElMessage.warning('请先添加食材')
    return
  }
  loading.value = true
  try {
    const res = await generateRecipeApi({
      ingredients: ingredientTags.value,
      allergy: conditions.allergy || undefined,
      taste: conditions.taste === '不限' ? undefined : conditions.taste,
      cuisine: conditions.cuisine === '不限' ? undefined : conditions.cuisine,
      healthGoal: conditions.healthGoal
    })
    store.setGenerated(res.data)
    ElMessage.success(`已生成 ${res.data.length} 套食谱`)
  } finally {
    loading.value = false
  }
}

/**
 * 是否已在购物清单
 */
function inShopping(dish) {
  return store.shoppingDishes.some((d) => d.dishName === dish.dishName)
}

/**
 * 加入/移出购物清单
 */
function toggleShopping(dish) {
  const added = store.toggleShopping(dish)
  ElMessage.success(added ? '已加入购物清单' : '已移出购物清单')
}

/**
 * 跳转改良Tab
 */
function goImprove(dish) {
  store.setImproveTarget(dish)
  emit('go-improve')
}
</script>

<style scoped>
/* 分区标题 */
.section {
  margin-bottom: 18px;
}

.section-title {
  font-weight: bold;
  margin-bottom: 10px;
  color: #303133;
}

/* 食材输入行 */
.ingredient-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.ingredient-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.ing-tag {
  font-size: 13px;
}

.hint {
  color: #909399;
  font-size: 13px;
}

/* 食谱卡片 */
.dish-col {
  margin-bottom: 16px;
}

.dish-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dish-name {
  font-weight: bold;
  font-size: 15px;
}

.dish-desc {
  color: #606266;
  font-size: 13px;
  margin-bottom: 8px;
}

.dish-meta {
  display: flex;
  gap: 16px;
  align-items: center;
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.ing-table {
  margin-bottom: 10px;
}

/* 步骤列表 */
.steps {
  padding-left: 20px;
  margin: 0 0 10px;
}

.steps li {
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
}

.dish-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>