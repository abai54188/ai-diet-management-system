<template>
  <div class="generate-tab">
    <!-- 生成方式切换: 按食材生成 / 直接选菜品分析(集成模式隐藏) -->
    <div v-if="!hideInput" class="section">
      <div class="section-title">生成方式</div>
      <el-radio-group v-model="mode">
        <el-radio-button value="ingredient">按食材生成菜谱</el-radio-button>
        <el-radio-button value="dish">直接选想吃的菜品</el-radio-button>
      </el-radio-group>
    </div>

    <!-- ===== 模式一: 按食材生成菜谱(选择食材 -> AI生成多套菜单) ===== -->
    <template v-if="mode === 'ingredient'">
      <!-- 食材输入区(集成模式下由外部搜索区选材) -->
      <div v-if="!hideInput" class="section">
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
          <el-form-item v-if="!hideInput">
            <el-button type="primary" :loading="loading" :disabled="!ingredientTags.length" @click="handleGenerate">
              <el-icon><MagicStick /></el-icon>&nbsp;生成食谱
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </template>

    <!-- ===== 模式二: 直接选想吃的菜品(AI分析热量+给出做法) ===== -->
    <template v-else>
      <div class="section">
        <div class="section-title">想吃的菜品</div>
        <div class="ingredient-row">
          <el-input
            v-model="dishKeyword"
            placeholder="输入想吃的菜品，如: 宫保鸡丁、番茄炒蛋"
            clearable
            style="width: 320px"
            @keyup.enter="handleAnalyzeDish"
          />
          <el-button type="primary" :loading="loading" :disabled="!dishKeyword.trim()" @click="handleAnalyzeDish">
            <el-icon><MagicStick /></el-icon>&nbsp;分析热量与做法
          </el-button>
        </div>
        <div class="hint" style="margin-top: 8px">AI 将还原这道菜的食材组成、计算热量，并给出详细做法，可加入购物清单或收藏</div>
      </div>
    </template>

    <!-- 生成的食谱(多套菜单, 每套含多道菜品) -->
    <el-empty v-if="!store.generatedMenus.length && !loading" :description="emptyText" />
    <div v-loading="loading">
      <el-collapse v-if="store.generatedMenus.length" v-model="activeMenus" class="menu-collapse">
        <el-collapse-item v-for="(menu, mi) in store.generatedMenus" :key="menu.menuName" :name="mi">
          <template #title>
            <div class="menu-title">
              <span class="menu-name">{{ menu.menuName }}</span>
              <el-tag size="small" type="info">{{ (menu.dishes || []).length }}道菜品</el-tag>
              <el-button type="success" link size="small" @click.stop="addMenuToShopping(menu)">整套餐加入购物清单</el-button>
            </div>
          </template>
          <div class="menu-desc">{{ menu.description }}</div>

          <!-- 菜单内每道菜品卡片 -->
          <div class="dish-list">
            <div v-for="dish in menu.dishes" :key="dish.dishName" class="dish-card-wrap">
              <el-card shadow="hover" class="dish-card">
                <div class="dish-header">
                  <span class="dish-name">{{ dish.dishName }}</span>
                  <el-tag type="warning" effect="dark">{{ dish.calorie }} kcal</el-tag>
                </div>
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
                <!-- 详细做法 -->
                <div class="steps-title"><el-icon><Notebook /></el-icon> 详细做法</div>
                <ol class="steps">
                  <li v-for="(s, i) in dish.steps" :key="i">
                    <span class="step-no">{{ i + 1 }}</span>
                    <span class="step-text">{{ s }}</span>
                  </li>
                </ol>
                <!-- 操作: 单道菜品可加入购物清单 / 收藏 / 改良 -->
                <div class="dish-actions">
                  <el-button :type="inShopping(dish) ? 'success' : 'default'" size="small" @click="toggleShopping(dish)">
                    {{ inShopping(dish) ? '已加入购物清单' : '加入购物清单' }}
                  </el-button>
                  <el-button :type="isCollected(dish) ? 'warning' : 'default'" size="small" @click="toggleCollect(dish)">
                    <el-icon><Star /></el-icon>&nbsp;{{ isCollected(dish) ? '已收藏' : '收藏' }}
                  </el-button>
                  <el-button type="primary" plain size="small" @click="goImprove(dish)">改良此菜品</el-button>
                </div>
              </el-card>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { foodSuggestApi } from '@/api/food'
import { generateRecipeApi, collectAiDishApi, analyzeDishApi } from '@/api/aiRecipe'
import { folderApi } from '@/api/community'
import { useRecipeStore } from '@/stores/recipe'

const emit = defineEmits(['go-improve'])

/**
 * 外部(如营养查询页)可注入食材并触发自动生成:
 *  - ingredients: 已选食材名称数组
 *  - trigger: 每次自增即触发一次填充+生成(去重后仍触发)
 */
const props = defineProps({
  ingredients: { type: Array, default: () => [] },
  trigger: { type: Number, default: 0 },
  // 集成模式: 隐藏顶部"生成方式/食材输入", 仅保留生成条件与结果(由外部搜索区选材后触发)
  hideInput: { type: Boolean, default: false }
})

const store = useRecipeStore()
const route = useRoute()

// ============ 食材标签输入 ============
const keyword = ref('')
const ingredientTags = ref(['鸡胸肉', '西兰花'])

/**
 * 从营养查询页跳转时, query 携带 ingredients(逗号分隔的食材名)
 * 用 watch 监听: 既能覆盖首次挂载(immediate), 也能覆盖同路由多次跳转(组件复用时 query 变化仍触发)
 */
watch(
  () => route.query.ingredients,
  async (val) => {
    if (val) {
      const names = String(val).split(',').filter(Boolean)
      if (names.length) {
        ingredientTags.value = names
        await handleGenerate()
      }
    }
  },
  { immediate: true }
)

// 合并页场景: 外部已选食材 -> 注入标签并自动生成
watch(
  () => props.trigger,
  async (val) => {
    if (val > 0 && props.ingredients && props.ingredients.length) {
      ingredientTags.value = [...props.ingredients]
      await handleGenerate()
    }
  },
  { immediate: true }
)

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

// 生成方式: ingredient-按食材生成 / dish-直接选菜品分析
const mode = ref('ingredient')
// 想吃的菜品输入
const dishKeyword = ref('')

// 空状态提示随模式变化
const emptyText = computed(() =>
  mode.value === 'ingredient'
    ? '添加食材并点击生成，AI将为您设计多套菜单，每套含多道菜品并附详细做法'
    : '输入想吃的菜品并点击分析，AI将还原食材、计算热量并给出详细做法'
)

// 默认展开第一套菜单
const activeMenus = ref([0])

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
 * 直接选菜品: AI还原食材+计算热量+给出详细做法, 以单菜单展示(可加购/收藏/改良)
 */
async function handleAnalyzeDish() {
  const name = dishKeyword.value.trim()
  if (!name) {
    ElMessage.warning('请输入想吃的菜品')
    return
  }
  loading.value = true
  try {
    const res = await analyzeDishApi(name)
    const dish = res.data
    if (!dish || !dish.ingredients) {
      ElMessage.warning('AI 未识别该菜品，请换个更具体的名称')
      return
    }
    store.setGenerated([{
      menuName: `「${name}」热量与做法`,
      description: dish.description || '单道菜品分析：还原食材组成、计算热量并给出详细做法',
      dishes: [dish]
    }])
    activeMenus.value = [0]
    ElMessage.success('分析完成')
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
 * 整套餐内所有菜品加入购物清单
 */
function addMenuToShopping(menu) {
  const dishes = menu.dishes || []
  if (!dishes.length) {
    ElMessage.warning('该套餐暂无菜品')
    return
  }
  const added = store.addDishesToShopping(dishes)
  ElMessage.success(added ? `已将 ${added} 道菜品加入购物清单` : '菜品已全部在购物清单中')
}

// ============ 菜品收藏 ============
// 已收藏的菜名列表
const collected = ref([])

/**
 * 加载收藏夹, 标记已收藏的菜名
 */
async function loadCollected() {
  try {
    const res = await folderApi()
    const folder = res.data || {}
    collected.value = Object.values(folder).flat().map((i) => i.recipeName).filter(Boolean)
  } catch (e) {
    collected.value = []
  }
}

/**
 * 是否已收藏某道菜
 */
function isCollected(dish) {
  return collected.value.includes(dish.dishName)
}

/**
 * 收藏/取消收藏某道菜(后端落库为用户食谱并写入收藏夹)
 */
async function toggleCollect(dish) {
  try {
    const res = await collectAiDishApi({
      dishName: dish.dishName,
      description: dish.description,
      difficulty: dish.difficulty,
      cookingTime: dish.cookingTime,
      steps: dish.steps || [],
      ingredients: (dish.ingredients || []).map((i) => ({ foodName: i.foodName, weight: i.weight })),
      calorie: dish.calorie
    }, 'AI生成食谱')
    const collectedNow = res.data?.collected
    if (collectedNow) {
      if (!collected.value.includes(dish.dishName)) collected.value.push(dish.dishName)
      ElMessage.success('已收藏，可在食谱收藏夹查看')
    } else {
      collected.value = collected.value.filter((n) => n !== dish.dishName)
      ElMessage.success('已取消收藏')
    }
  } catch (e) {
    ElMessage.error('收藏操作失败')
  }
}

// 挂载时加载已收藏状态
onMounted(loadCollected)

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

/* 菜单折叠区 */
.menu-collapse {
  border-top: none;
}

.menu-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: bold;
}

.menu-name {
  font-size: 15px;
  color: var(--brand-700, #3B8763);
}

.menu-desc {
  color: #606266;
  font-size: 13px;
  margin-bottom: 12px;
}

/* 菜单内菜品网格 */
.dish-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 14px;
}

.dish-card-wrap {
  min-width: 0;
}

.dish-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
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

/* 详细做法 */
.steps-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 6px;
}

.steps {
  list-style: none;
  padding: 0;
  margin: 0 0 10px;
}

.steps li {
  display: flex;
  gap: 8px;
  font-size: 13px;
  color: #4c5258;
  line-height: 1.8;
  padding: 5px 8px;
  border-left: 2px solid var(--brand-200, #BFD9C9);
  margin-bottom: 4px;
  background: rgba(59, 135, 99, 0.04);
  border-radius: 0 6px 6px 0;
}

.step-no {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  margin-top: 3px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-500, #3B8763);
  color: #fff;
  border-radius: 50%;
  font-size: 11px;
  font-family: var(--font-mono);
}

.step-text {
  flex: 1;
}

.dish-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>