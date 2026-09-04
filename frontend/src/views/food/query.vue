<template>
  <div class="food-query-page">
  <el-card shadow="never" class="recipe-panel">
    <template #header>
      <div class="panel-header">
        <span>食谱中心</span>
      </div>
    </template>

    <el-tabs v-model="recipeTab">
      <!-- 生成食谱: 集成食材搜索 + 已选食材 + 生成结果于一体 -->
      <el-tab-pane label="生成食谱" name="generate">
        <div class="gen-workspace">
          <div class="selection-area">
            <!-- 左: 食材搜索 + 菜品快捷分析 + 收藏 -->
            <el-card shadow="never" class="left-panel">
              <template #header>
                <div class="panel-header">
                  <span>食材搜索</span>
                  <el-tag type="success" size="small">本地成分表数据</el-tag>
                </div>
              </template>

              <!-- 想吃的菜品快捷分析 -->
              <div class="dish-quick">
                <el-input v-model="dishQuick" placeholder="输入想吃的菜品，如: 宫保鸡丁" clearable
                  @keyup.enter="analyzeQuick" />
                <el-button type="primary" :loading="dishAnalyzing" @click="analyzeQuick">分析</el-button>
              </div>

              <!-- 搜索输入框(带联想补全) -->
              <el-autocomplete
                v-model="searchKeyword"
                :fetch-suggestions="querySearch"
                placeholder="输入食材名称，如: 鸡蛋 / 土豆 / 三文鱼"
                clearable
                style="width: 100%"
                @select="handleSuggestSelect"
              >
                <template #default="{ item }">
                  <div class="suggest-item">
                    <span class="suggest-name">{{ item.foodName }}</span>
                    <span class="suggest-cal">{{ item.calorie }} kcal/100g</span>
                  </div>
                </template>
              </el-autocomplete>

              <!-- 分类快捷筛选 -->
              <div class="category-tags">
                <el-tag
                  v-for="cat in categories"
                  :key="cat"
                  :type="activeCategory === cat ? 'primary' : 'info'"
                  :effect="activeCategory === cat ? 'dark' : 'plain'"
                  class="cat-tag"
                  @click="toggleCategory(cat)"
                >
                  {{ cat }}
                </el-tag>
              </div>

              <!-- 搜索结果列表 -->
              <el-table :data="searchResults" v-loading="searchLoading" size="small" height="300"
                empty-text="输入关键词或选择分类查看食材">
                <el-table-column prop="foodName" label="食材" min-width="110" show-overflow-tooltip />
                <el-table-column prop="category" label="分类" width="85" />
                <el-table-column label="热量" width="95" align="center">
                  <template #default="{ row }">
                    <span class="cal-text">{{ row.calorie }}</span>
                    <span class="unit"> kcal</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="60" align="center">
                  <template #default="{ row }">
                    <el-button type="primary" link size="small" @click="openAddDialog(row)">添加</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <!-- 搜索分页 -->
              <el-pagination
                v-model:current-page="searchPage.current"
                :page-size="searchPage.size"
                :total="searchPage.total"
                layout="total, prev, pager, next"
                small
                class="search-pagination"
                @current-change="handleSearch"
              />

              <!-- 收藏夹 -->
              <el-divider content-position="left">
                <el-icon><Star /></el-icon> 我的收藏夹
              </el-divider>
              <div v-if="collectList.length === 0" class="collect-empty">
                暂无收藏，点击食材行的星星即可收藏
              </div>
              <div v-else class="collect-list">
                <div v-for="c in collectList" :key="c.id" class="collect-item">
                  <div class="collect-info">
                    <span class="collect-name">{{ c.foodName }}</span>
                    <span class="collect-cal">{{ c.fixedWeight }}g / {{ (c.calorie * c.fixedWeight / 100).toFixed(1) }} kcal</span>
                  </div>
                  <div class="collect-actions">
                    <el-button type="primary" link size="small" @click="addFromCollect(c)">添加</el-button>
                    <el-button type="danger" link size="small" @click="removeCollect(c)">删除</el-button>
                  </div>
                </div>
              </div>
            </el-card>

            <!-- 右: 已选食材与营养汇总 -->
            <el-card shadow="never" class="right-panel">
              <template #header>
                <div class="panel-header">
                  <span>已选食材（{{ selectedItems.length }}项）</span>
                  <div>
                    <el-button v-if="selectedItems.length" type="primary" size="small" @click="goGenerate">
                      用这些食材生成食谱
                    </el-button>
                    <el-button v-if="selectedItems.length" type="danger" link size="small" @click="clearAll">
                      清空列表
                    </el-button>
                  </div>
                </div>
              </template>

              <!-- 操作提示 -->
              <el-alert v-if="!selectedItems.length" type="info" :closable="false" class="empty-tip"
                title="在左侧搜索并添加食材，然后在下方选择生成条件，点击「生成食谱」即可由 AI 自动生成多套菜单" />

              <!-- 汇总统计卡片 -->
              <div class="summary-cards">
                <div class="summary-card cal">
                  <div class="summary-value">{{ calculateResult.totalCalorie ?? '0.0' }}</div>
                  <div class="summary-label">总热量(kcal)</div>
                </div>
                <div class="summary-card protein">
                  <div class="summary-value">{{ calculateResult.totalProtein ?? '0.0' }}</div>
                  <div class="summary-label">蛋白质(g)</div>
                </div>
                <div class="summary-card carb">
                  <div class="summary-value">{{ calculateResult.totalCarbohydrate ?? '0.0' }}</div>
                  <div class="summary-label">碳水(g)</div>
                </div>
                <div class="summary-card fat">
                  <div class="summary-value">{{ calculateResult.totalFat ?? '0.0' }}</div>
                  <div class="summary-label">脂肪(g)</div>
                </div>
              </div>

              <!-- 已选食材表格 -->
              <el-table :data="calculateResult.items" v-loading="calculateLoading" size="small" height="360"
                empty-text="从左侧搜索并添加食材，开始计算营养摄入">
                <el-table-column prop="foodName" label="食材" min-width="100" show-overflow-tooltip />
                <el-table-column label="重量(g)" width="110">
                  <template #default="{ row }">
                    <el-input-number v-model="row.weight" :min="1" :max="10000" :step="10" size="small"
                      controls-position="right" style="width: 95px" @change="recalculate" />
                  </template>
                </el-table-column>
                <el-table-column prop="calorie" label="热量(kcal)" width="95" align="center" />
                <el-table-column prop="protein" label="蛋白(g)" width="80" align="center" />
                <el-table-column prop="carbohydrate" label="碳水(g)" width="80" align="center" />
                <el-table-column prop="fat" label="脂肪(g)" width="80" align="center" />
                <el-table-column label="操作" width="110" align="center">
                  <template #default="{ row }">
                    <el-button type="primary" link size="small" @click="showDetail(row.foodId)">详情</el-button>
                    <el-button type="warning" link size="small" @click="toggleCollect(row)">收藏</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>

          <!-- 生成结果区: 条件 + 食材注入 + 结果渲染(GenerateTab隐藏重复输入, 由上方搜索/已选驱动) -->
          <GenerateTab :ingredients="selectedNames" :trigger="recipeTrigger" hide-input
            @go-improve="recipeTab = 'improve'" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="一周食谱规划" name="week">
        <WeekTab />
      </el-tab-pane>
      <el-tab-pane label="购物清单" name="shopping">
        <ShoppingTab />
      </el-tab-pane>
      <el-tab-pane label="食谱改良" name="improve">
        <ImproveTab />
      </el-tab-pane>
    </el-tabs>
  </el-card>

    <!-- 添加食材弹窗(设置重量) -->
    <el-dialog v-model="addDialog.visible" title="添加食材" width="360px" :close-on-click-modal="false">
      <div class="add-dialog-body">
        <div class="add-food-name">{{ addDialog.food?.foodName }}</div>
        <div class="add-food-info">
          {{ addDialog.food?.category }} | {{ addDialog.food?.calorie }} kcal/100g
        </div>
        <el-form label-width="80px" style="margin-top: 16px">
          <el-form-item label="重量(g)">
            <el-input-number v-model="addDialog.weight" :min="1" :max="10000" :step="10" style="width: 100%" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="addDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdd">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 营养成分详情弹窗 -->
    <el-dialog v-model="detailDialog.visible" title="营养成分表" width="560px">
      <div v-if="detailDialog.food" v-loading="detailDialog.loading">
        <div class="detail-header">
          <span class="detail-name">{{ detailDialog.food.foodName }}</span>
          <el-tag size="small">{{ detailDialog.food.category }}</el-tag>
        </div>
        <div class="detail-alias" v-if="detailDialog.food.alias">别名: {{ detailDialog.food.alias }}</div>
        <div class="detail-note">以下数值均为每100g可食部含量</div>
        <el-descriptions :column="2" border size="small" class="detail-table">
          <el-descriptions-item label="热量(kcal)">
            <span class="hl">{{ detailDialog.food.calorie }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="蛋白质(g)">{{ detailDialog.food.protein }}</el-descriptions-item>
          <el-descriptions-item label="碳水化合物(g)">{{ detailDialog.food.carbohydrate }}</el-descriptions-item>
          <el-descriptions-item label="脂肪(g)">{{ detailDialog.food.fat }}</el-descriptions-item>
          <el-descriptions-item label="膳食纤维(g)">{{ detailDialog.food.dietaryFiber }}</el-descriptions-item>
          <el-descriptions-item label="维生素C(mg)">{{ detailDialog.food.vitaminC }}</el-descriptions-item>
          <el-descriptions-item label="维生素E(mg)">{{ detailDialog.food.vitaminE }}</el-descriptions-item>
          <el-descriptions-item label="维生素B1(mg)">{{ detailDialog.food.vitaminB1 }}</el-descriptions-item>
          <el-descriptions-item label="维生素B2(mg)">{{ detailDialog.food.vitaminB2 }}</el-descriptions-item>
          <el-descriptions-item label="钙(mg)">{{ detailDialog.food.calcium }}</el-descriptions-item>
          <el-descriptions-item label="铁(mg)">{{ detailDialog.food.iron }}</el-descriptions-item>
          <el-descriptions-item label="钠(mg)">{{ detailDialog.food.sodium }}</el-descriptions-item>
          <el-descriptions-item label="钾(mg)">{{ detailDialog.food.potassium }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  foodPageApi,
  foodSuggestApi,
  foodCalculateApi,
  foodDetailApi,
  collectListApi,
  collectSaveApi,
  collectDeleteApi
} from '@/api/food'
import { analyzeDishApi } from '@/api/aiRecipe'
import { useRecipeStore } from '@/stores/recipe'
import GenerateTab from '@/views/recipe/components/GenerateTab.vue'
import WeekTab from '@/views/recipe/components/WeekTab.vue'
import ShoppingTab from '@/views/recipe/components/ShoppingTab.vue'
import ImproveTab from '@/views/recipe/components/ImproveTab.vue'

// 全部食材分类
const categories = ['谷薯类', '蔬菜类', '水果类', '肉禽蛋类', '水产类', '奶豆类', '坚果类', '调料类', '饮品类', '加工食品类']

// ============ 搜索区状态 ============
const searchKeyword = ref('')
const activeCategory = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const searchPage = reactive({ current: 1, size: 8, total: 0 })

/**
 * 执行搜索(关键词 + 分类组合查询)
 */
async function handleSearch() {
  searchLoading.value = true
  try {
    const res = await foodPageApi({
      current: searchPage.current,
      size: searchPage.size,
      keyword: searchKeyword.value || undefined,
      category: activeCategory.value || undefined
    })
    searchResults.value = res.data.records
    searchPage.total = Number(res.data.total)
  } finally {
    searchLoading.value = false
  }
}

/**
 * 输入联想回调: 防抖后请求前5条建议
 */
let suggestTimer = null
function querySearch(keyword, cb) {
  clearTimeout(suggestTimer)
  if (!keyword || !keyword.trim()) {
    cb([])
    return
  }
  // 300ms 防抖，避免频繁请求
  suggestTimer = setTimeout(async () => {
    const res = await foodSuggestApi(keyword.trim())
    cb(res.data || [])
  }, 300)
}

/**
 * 选中联想项: 打开添加弹窗
 */
function handleSuggestSelect(item) {
  openAddDialog(item)
}

/**
 * 切换分类筛选
 */
function toggleCategory(cat) {
  activeCategory.value = activeCategory.value === cat ? '' : cat
  searchPage.current = 1
  handleSearch()
}

// ============ 添加食材弹窗 ============
const addDialog = reactive({
  visible: false,
  food: null,
  weight: 100
})

/**
 * 打开添加弹窗
 */
function openAddDialog(food) {
  addDialog.food = food
  addDialog.weight = 100
  addDialog.visible = true
}

/**
 * 确认添加: 加入已选列表并触发计算
 */
function confirmAdd() {
  const food = addDialog.food
  selectedItems.value.push({ foodId: food.id, foodName: food.foodName, weight: addDialog.weight })
  addDialog.visible = false
  recalculate()
}

// ============ 已选列表与计算 ============
const selectedItems = ref([])
const calculateResult = ref({})
const calculateLoading = ref(false)

// ============ 食谱中心(合并区) ============
// 当前激活的食谱Tab: generate / week / shopping / improve
const recipeTab = ref('generate')
// 触发计数: 每 +1 即让 GenerateTab 用当前已选食材重新生成
const recipeTrigger = ref(0)
// AI 食谱共享 store(菜品快捷分析结果写入, 由 GenerateTab 渲染)
const recipeStore = useRecipeStore()

// 想吃的菜品快捷分析
const dishQuick = ref('')
const dishAnalyzing = ref(false)

/**
 * 直接选菜品: AI分析热量+给做法, 结果写入共享store由GenerateTab渲染
 */
async function analyzeQuick() {
  const name = dishQuick.value.trim()
  if (!name) {
    ElMessage.warning('请输入想吃的菜品')
    return
  }
  dishAnalyzing.value = true
  try {
    const res = await analyzeDishApi(name)
    const dish = res.data
    if (!dish || !dish.ingredients) {
      ElMessage.warning('AI 未识别该菜品，请换个更具体的名称')
      return
    }
    recipeStore.setGenerated([{
      menuName: `「${name}」热量与做法`,
      description: dish.description || '单道菜品分析：还原食材组成、计算热量并给出详细做法',
      dishes: [dish]
    }])
    ElMessage.success('分析完成')
  } finally {
    dishAnalyzing.value = false
  }
}

// 已选食材名称, 供"生成食谱"Tab注入
const selectedNames = computed(() =>
  selectedItems.value.map((i) => i.foodName).filter(Boolean)
)

/**
 * 用已选食材生成食谱: 切到"生成食谱"Tab 并触发自动生成
 */
function goGenerate() {
  if (!selectedNames.value.length) {
    ElMessage.warning('请先添加食材')
    return
  }
  recipeTab.value = 'generate'
  recipeTrigger.value += 1
}

/**
 * 批量计算营养摄入(每次列表变化后重新请求)
 */
async function recalculate() {
  if (selectedItems.value.length === 0) {
    calculateResult.value = {}
    return
  }
  calculateLoading.value = true
  try {
    const res = await foodCalculateApi(selectedItems.value)
    calculateResult.value = res.data
    // 后端返回的 items 与 selectedItems 顺序一致，回写引用以保持输入框绑定
    res.data.items.forEach((itemVo, index) => {
      if (selectedItems.value[index]) {
        itemVo.weight = selectedItems.value[index].weight
      }
    })
  } finally {
    calculateLoading.value = false
  }
}

/**
 * 清空已选列表
 */
function clearAll() {
  selectedItems.value = []
  calculateResult.value = {}
}

// ============ 详情弹窗 ============
const detailDialog = reactive({
  visible: false,
  loading: false,
  food: null
})

/**
 * 查看食材完整营养成分表
 */
async function showDetail(foodId) {
  detailDialog.visible = true
  detailDialog.loading = true
  detailDialog.food = null
  try {
    const res = await foodDetailApi(foodId)
    detailDialog.food = res.data
  } finally {
    detailDialog.loading = false
  }
}

// ============ 收藏夹 ============
const collectList = ref([])

/**
 * 加载收藏夹列表
 */
async function loadCollects() {
  const res = await collectListApi()
  collectList.value = res.data || []
}

/**
 * 收藏/取消收藏已选列表中的食材
 */
async function toggleCollect(row) {
  const exist = collectList.value.find((c) => c.foodId === row.foodId)
  if (exist) {
    await ElMessageBox.confirm('该食材已在收藏夹中，是否删除收藏？', '提示', { type: 'warning' })
    await collectDeleteApi(exist.id)
    ElMessage.success('已取消收藏')
  } else {
    await collectSaveApi({ foodId: row.foodId, fixedWeight: row.weight })
    ElMessage.success('收藏成功')
  }
  loadCollects()
}

/**
 * 从收藏夹删除
 */
async function removeCollect(c) {
  await collectDeleteApi(c.id)
  ElMessage.success('已删除')
  loadCollects()
}

/**
 * 一键将收藏食材按固定重量加入计算列表
 */
function addFromCollect(c) {
  selectedItems.value.push({ foodId: c.foodId, foodName: c.foodName, weight: Number(c.fixedWeight) })
  recalculate()
  ElMessage.success(`已添加 ${c.foodName}`)
}

// 页面初始化: 加载默认食材列表与收藏夹
onMounted(() => {
  handleSearch()
  loadCollects()
})
</script>

<style scoped>
/* 页面布局 */
.food-query-page {
  margin: 0;
}

.recipe-panel {
  width: 100%;
}

/* 食谱中心内嵌生成工作区 */
.gen-workspace {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.selection-area {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.left-panel {
  width: 44%;
  min-width: 400px;
}

.right-panel {
  flex: 1;
  min-width: 420px;
}

/* 菜品快捷分析 */
.dish-quick {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.dish-quick .el-input {
  flex: 1;
}

/* 空状态操作提示 */
.empty-tip {
  margin-bottom: 12px;
}

/* 面板标题 */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: bold;
}

/* 分类标签 */
.category-tags {
  margin: 12px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cat-tag {
  cursor: pointer;
}

/* 搜索分页 */
.search-pagination {
  margin-top: 10px;
  justify-content: flex-end;
}

/* 联想项 */
.suggest-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.suggest-cal {
  color: #909399;
  font-size: 12px;
}

/* 汇总统计卡片 */
.summary-cards {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  flex: 1;
  border-radius: 8px;
  padding: 14px 10px;
  text-align: center;
  color: #fff;
}

.summary-card.cal {
  background: #FDF1E9; color: #A85A2E;
}

.summary-card.protein {
  background: #FAEDEC; color: #A03F38;
}

.summary-card.carb {
  background: #EBF1F7; color: #35608C;
}

.summary-card.fat {
  background: #FAF2DE; color: #9A6E1E;
}

.summary-value {
  font-size: 26px;
  font-family: var(--font-mono);
  font-weight: bold;
}

.summary-label {
  font-size: 12px;
  opacity: 0.9;
  margin-top: 4px;
}

/* 热量文本 */
.cal-text {
  color: var(--data-cal);
  font-weight: bold;
}

.unit {
  color: #909399;
  font-size: 12px;
}

/* 收藏夹 */
.collect-empty {
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 12px 0;
}

.collect-list {
  max-height: 200px;
  overflow-y: auto;
}

.collect-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  border-bottom: 1px dashed #ebeef5;
}

.collect-item:last-child {
  border-bottom: none;
}

.collect-info {
  display: flex;
  flex-direction: column;
}

.collect-name {
  font-size: 14px;
  color: #303133;
}

.collect-cal {
  font-size: 12px;
  color: #909399;
}

/* 添加弹窗 */
.add-food-name {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.add-food-info {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

/* 详情弹窗 */
.detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.detail-name {
  font-size: 18px;
  font-weight: bold;
}

.detail-alias {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.detail-note {
  color: var(--data-fat);
  font-size: 12px;
  margin-bottom: 12px;
}

.detail-table .hl {
  color: var(--data-cal);
  font-weight: bold;
}
</style>