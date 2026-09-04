<template>
  <div class="publish-page">
    <el-card shadow="never" class="publish-card">
      <template #header>发布动态</template>
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="一句话概括(100字内)" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="分享你的健康饮食心得(5000字内), 发布时自动过滤敏感词" />
        </el-form-item>
        <el-form-item label="图片">
          <!-- 图片URL模拟上传: 实际项目接入OSS后替换 -->
          <div class="img-list">
            <div v-for="(img, i) in form.images" :key="i" class="img-item">
              <el-image :src="img" fit="cover" class="img-thumb" />
              <el-icon class="img-del" @click="form.images.splice(i, 1)"><CircleClose /></el-icon>
            </div>
            <div class="img-add" @click="addImage">
              <el-icon :size="22"><Plus /></el-icon>
            </div>
          </div>
          <div class="img-tip">演示环境以图片URL模拟上传(支持 https://... 地址)</div>
        </el-form-item>
        <el-form-item label="关联食谱">
          <el-select v-model="form.recipeId" placeholder="可选: 关联我的食谱" clearable filterable style="width: 320px">
            <el-option v-for="r in myRecipes" :key="r.id" :label="r.recipeName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="publishing" :disabled="!form.title" @click="handlePublish">
            发布
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { publishApi, recipePageApi } from '@/api/community'

const router = useRouter()

// 发布表单
const form = reactive({
  title: '',
  content: '',
  images: [],
  recipeId: null
})
const publishing = ref(false)

// 我的食谱(供关联选择)
const myRecipes = ref([])

onMounted(async () => {
  const res = await recipePageApi({ current: 1, size: 50 })
  myRecipes.value = res.data.records || []
})

/**
 * 模拟添加图片(URL方式)
 */
async function addImage() {
  const { value } = await ElMessageBox.prompt('请输入图片URL(演示用)', '添加图片', {
    inputPlaceholder: 'https://example.com/food.jpg'
  }).catch(() => ({ value: null }))
  if (value) {
    form.images.push(value)
  }
}

/**
 * 发布动态
 */
async function handlePublish() {
  publishing.value = true
  try {
    await publishApi({
      title: form.title,
      content: form.content,
      images: form.images,
      recipeId: form.recipeId || undefined
    })
    ElMessage.success('发布成功')
    router.push('/community/square')
  } finally {
    publishing.value = false
  }
}
</script>

<style scoped>
.publish-card {
  max-width: 760px;
  margin: 0 auto;
}

/* 图片列表 */
.img-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.img-item {
  position: relative;
}

.img-thumb {
  width: 120px;
  height: 90px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.img-del {
  position: absolute;
  top: -6px;
  right: -6px;
  color: var(--data-protein);
  cursor: pointer;
  background: #fff;
  border-radius: 50%;
}

.img-add {
  width: 120px;
  height: 90px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  cursor: pointer;
}

.img-add:hover {
  border-color: var(--brand-400);
  color: var(--brand-500);
}

.img-tip {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 6px;
}
</style>