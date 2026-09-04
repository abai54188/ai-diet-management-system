<template>
  <div class="favorites-page">
    <el-card shadow="never">
      <template #header>
        <div class="fav-header">
          <span>食谱收藏夹</span>
          <el-radio-group v-if="categories.length" v-model="activeCategory" size="small">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button v-for="c in categories" :key="c" :value="c">{{ c }}</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-empty v-if="!filtered.length" description="暂无收藏，去食谱生成页收藏喜欢的食谱吧" />
      <el-row :gutter="14" v-else>
        <el-col :span="8" v-for="item in filtered" :key="item.collectId" class="fav-col">
          <el-card shadow="hover" class="fav-card">
            <div class="fav-name">{{ item.recipeName || '(食谱已删除)' }}</div>
            <div class="fav-meta">
              <el-tag size="small" type="info">{{ item.category }}</el-tag>
              <span v-if="item.caloriePerServing">{{ item.caloriePerServing }} kcal/份</span>
            </div>
            <div class="fav-meta2">
              难度{{ '★'.repeat(item.difficulty || 1) }} · {{ item.cookingTime }}分钟
            </div>
            <div class="fav-actions">
              <el-button size="small" type="primary" plain @click="goGenerate">去做这道菜</el-button>
              <el-button size="small" type="danger" plain @click="removeCollect(item)">取消收藏</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { folderApi, collectRecipeApi } from '@/api/community'

const router = useRouter()

// 收藏夹数据(分类分组)
const folder = ref({})
const activeCategory = ref('')

// 全部分类
const categories = computed(() => Object.keys(folder.value))

// 按当前分类过滤
const filtered = computed(() => {
  if (!activeCategory.value) {
    return Object.values(folder.value).flat()
  }
  return folder.value[activeCategory.value] || []
})

onMounted(loadFolder)

/**
 * 加载收藏夹
 */
async function loadFolder() {
  const res = await folderApi()
  folder.value = res.data || {}
}

/**
 * 取消收藏
 */
async function removeCollect(item) {
  await collectRecipeApi(item.recipeId, item.category)
  ElMessage.success('已取消收藏')
  await loadFolder()
}

/** 跳转食谱页 */
function goGenerate() {
  router.push('/recipe/generate')
}
</script>

<style scoped>
.fav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.fav-col {
  margin-bottom: 14px;
}

.fav-name {
  font-weight: bold;
  font-size: 15px;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fav-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}

.fav-meta2 {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.fav-actions {
  display: flex;
  gap: 8px;
}
</style>