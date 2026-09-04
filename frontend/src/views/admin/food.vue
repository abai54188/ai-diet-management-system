<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="food-header">
          <span>食材营养库管理（{{ page.total }}条）</span>
          <el-button type="primary" @click="openEdit(null)">
            <el-icon><Plus /></el-icon>&nbsp;新增食材
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" class="search-bar">
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="食材名称/别名" clearable style="width: 200px"
            @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.category" placeholder="全部分类" clearable style="width: 140px">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="foods" v-loading="loading" size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="foodName" label="食材名称" width="130" />
        <el-table-column prop="alias" label="别名" min-width="150" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="90" />
        <el-table-column prop="calorie" label="热量" width="80" align="center">
          <template #default="{ row }">{{ row.calorie }}</template>
        </el-table-column>
        <el-table-column prop="protein" label="蛋白" width="70" align="center" />
        <el-table-column prop="carbohydrate" label="碳水" width="70" align="center" />
        <el-table-column prop="fat" label="脂肪" width="70" align="center" />
        <el-table-column label="操作" width="130" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="page.current" :page-size="page.size" :total="page.total"
        layout="total, prev, pager, next" class="table-pagination" @current-change="load" />
    </el-card>

    <!-- 编辑/新增弹窗 -->
    <el-dialog v-model="editDialog.visible" :title="editDialog.food.id ? '编辑食材' : '新增食材'" width="640px">
      <el-form :model="editDialog.food" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="食材名称" required>
              <el-input v-model="editDialog.food.foodName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="别名(分号)">
              <el-input v-model="editDialog.food.alias" placeholder="别名1;别名2" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="分类" required>
          <el-select v-model="editDialog.food.category" style="width: 220px">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="热量kcal"><el-input-number v-model="editDialog.food.calorie" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="蛋白g"><el-input-number v-model="editDialog.food.protein" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="碳水g"><el-input-number v-model="editDialog.food.carbohydrate" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="脂肪g"><el-input-number v-model="editDialog.food.fat" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="纤维g"><el-input-number v-model="editDialog.food.dietaryFiber" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="钙mg"><el-input-number v-model="editDialog.food.calcium" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="铁mg"><el-input-number v-model="editDialog.food.iron" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="钠mg"><el-input-number v-model="editDialog.food.sodium" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="钾mg"><el-input-number v-model="editDialog.food.potassium" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="editDialog.saving" @click="saveFood">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminFoodPageApi, adminFoodSaveApi, adminFoodUpdateApi, adminFoodDeleteApi } from '@/api/community'

const categories = ['谷薯类', '蔬菜类', '水果类', '肉禽蛋类', '水产类', '奶豆类', '坚果类', '调料类', '饮品类', '加工食品类']

const foods = ref([])
const loading = ref(false)
const query = reactive({ keyword: '', category: null })
const page = reactive({ current: 1, size: 10, total: 0 })

onMounted(load)

/**
 * 加载食材分页
 */
async function load() {
  loading.value = true
  try {
    const res = await adminFoodPageApi({
      current: page.current, size: page.size,
      keyword: query.keyword || undefined,
      category: query.category || undefined
    })
    foods.value = res.data.records
    page.total = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

/**
 * 打开编辑弹窗(row=null新增)
 */
const editDialog = reactive({ visible: false, saving: false, food: {} })
function openEdit(row) {
  editDialog.food = row
    ? { ...row }
    : { foodName: '', alias: '', category: '蔬菜类', calorie: 0, protein: 0, carbohydrate: 0, fat: 0,
        dietaryFiber: 0, calcium: 0, iron: 0, sodium: 0, potassium: 0 }
  editDialog.visible = true
}

/**
 * 保存食材(新增或编辑)
 */
async function saveFood() {
  const f = editDialog.food
  if (!f.foodName || !f.category) {
    ElMessage.warning('请填写食材名称与分类')
    return
  }
  editDialog.saving = true
  try {
    if (f.id) {
      await adminFoodUpdateApi(f)
    } else {
      await adminFoodSaveApi(f)
    }
    ElMessage.success('保存成功')
    editDialog.visible = false
    await load()
  } finally {
    editDialog.saving = false
  }
}

/**
 * 删除食材
 */
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.foodName}」吗？删除后用户端不可见`, '删除食材', { type: 'warning' })
  await adminFoodDeleteApi(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.food-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-bar {
  margin-bottom: 6px;
}

.table-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>