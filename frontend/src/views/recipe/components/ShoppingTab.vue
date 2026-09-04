<template>
  <div class="shopping-tab">
    <!-- 空状态 -->
    <el-empty
      v-if="!store.shoppingDishes.length"
      description="暂无购物清单，请到「生成食谱」或「一周食谱规划」中将菜品加入购物清单"
    />

    <template v-else>
      <!-- 清单头部 -->
      <div class="list-header">
        <span>已选 {{ store.shoppingDishes.length }} 道菜品，已购 {{ boughtCount }}/{{ totalCount }} 项食材</span>
        <div>
          <el-button size="small" @click="clearBought">清除已购标记</el-button>
          <el-button size="small" type="danger" @click="handleClear">清空清单</el-button>
        </div>
      </div>

      <!-- 已选菜品一览 -->
      <div class="dish-tags">
        <el-tag v-for="d in store.shoppingDishes" :key="d.dishName" closable @close="removeDish(d)">
          {{ d.dishName }}
        </el-tag>
      </div>

      <!-- 分类清单 -->
      <el-row :gutter="14" v-loading="loading">
        <el-col :span="6" v-for="group in result?.categories || []" :key="group.category" class="group-col">
          <el-card shadow="hover" class="group-card">
            <template #header>
              <div class="group-header">
                <el-icon :size="16" :color="iconColor(group.category)"><component :is="iconName(group.category)" /></el-icon>
                <span>{{ group.category }}</span>
                <el-tag size="small" type="info">{{ group.items.length }}项</el-tag>
              </div>
            </template>
            <div v-for="item in group.items" :key="item.foodName" class="shop-item">
              <el-checkbox :model-value="!!purchased[item.foodName]" @change="purchased[item.foodName] = !purchased[item.foodName]">
                <span :class="{ bought: purchased[item.foodName] }">{{ item.foodName }}</span>
              </el-checkbox>
              <span class="item-weight">{{ item.weight }}g</span>
              <el-tag v-if="!item.matched" type="danger" size="small">未匹配</el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { shoppingListApi } from '@/api/aiRecipe'
import { useRecipeStore } from '@/stores/recipe'

const store = useRecipeStore()

// 汇总结果(后端聚合)
const result = ref(null)
const loading = ref(false)

// 已购标记(本地状态, 按食材名记录)
const purchased = reactive({})

/**
 * 调用后端汇总购物清单(跨菜品聚合+分类)
 */
async function loadList() {
  if (!store.shoppingDishes.length) {
    result.value = null
    return
  }
  loading.value = true
  try {
    const dishes = store.shoppingDishes.map((d) => ({
      dishName: d.dishName,
      ingredients: (d.ingredients || []).map((i) => ({ foodName: i.foodName, weight: i.weight }))
    }))
    const res = await shoppingListApi({ dishes })
    result.value = res.data
  } finally {
    loading.value = false
  }
}

// 购物清单变化时自动重新汇总
watch(() => store.shoppingDishes, loadList, { deep: true })
onMounted(loadList)

// 已购/总项数统计
const totalCount = computed(() =>
  (result.value?.categories || []).reduce((acc, g) => acc + g.items.length, 0)
)
const boughtCount = computed(() =>
  (result.value?.categories || []).reduce(
    (acc, g) => acc + g.items.filter((i) => purchased[i.foodName]).length,
    0
  )
)

/**
 * 分组图标
 */
function iconName(category) {
  return { 蔬菜: 'Apple', 肉类水产: 'Drumstick', 调料: 'Sugar' }[category] || 'ShoppingBag'
}

/**
 * 分组图标颜色
 */
function iconColor(category) {
  return { 蔬菜: '#3B8763', 肉类水产: '#C95850', 调料: '#D9A13B' }[category] || '#97A29A'
}

/**
 * 移除菜品
 */
function removeDish(dish) {
  store.toggleShopping(dish)
}

/**
 * 清空清单
 */
async function handleClear() {
  await ElMessageBox.confirm('确定清空购物清单吗？', '提示', { type: 'warning' })
  store.clearShopping()
  Object.keys(purchased).forEach((k) => delete purchased[k])
  ElMessage.success('清单已清空')
}

/**
 * 清除已购标记
 */
function clearBought() {
  Object.keys(purchased).forEach((k) => delete purchased[k])
}
</script>

<style scoped>
/* 头部统计 */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: #303133;
  font-size: 14px;
}

/* 菜品标签 */
.dish-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

/* 分组卡片 */
.group-col {
  margin-bottom: 14px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
}

/* 食材项 */
.shop-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 0;
  border-bottom: 1px dashed #ebeef5;
}

.shop-item:last-child {
  border-bottom: none;
}

.item-weight {
  color: #606266;
  font-size: 13px;
}

/* 已购划线 */
.bought {
  text-decoration: line-through;
  color: #c0c4cc;
}
</style>