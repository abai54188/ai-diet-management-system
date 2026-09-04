<template>
  <div class="profile-page" v-if="profile">
    <el-row :gutter="16">
      <!-- 左侧: 用户卡片 -->
      <el-col :span="7">
        <el-card shadow="never">
          <div class="user-head">
            <el-avatar :size="72" class="big-avatar">{{ (profile.nickname || '?').charAt(0) }}</el-avatar>
            <div class="user-name">{{ profile.nickname }}</div>
            <div class="user-account">@{{ profile.username }}</div>
            <!-- 关注按钮 -->
            <el-button v-if="profile.followed !== null" :type="profile.followed ? 'default' : 'primary'"
              style="width: 100%; margin-top: 12px" @click="handleFollow">
              {{ profile.followed ? '已关注' : '+ 关注' }}
            </el-button>
          </div>
          <el-divider />
          <div class="stat-row">
            <div class="stat"><b>{{ profile.postCount }}</b><span>动态</span></div>
            <div class="stat"><b>{{ profile.followerCount }}</b><span>粉丝</span></div>
            <div class="stat"><b>{{ profile.followingCount }}</b><span>关注</span></div>
          </div>
          <el-divider />
          <el-button style="width: 100%" @click="$router.push('/community/favorites')">我的食谱收藏夹</el-button>
        </el-card>
      </el-col>

      <!-- 右侧: 动态列表 -->
      <el-col :span="17">
        <el-card shadow="never">
          <template #header>TA的动态</template>
          <div v-loading="loading">
            <el-empty v-if="!posts.length" description="暂无动态" />
            <div v-for="post in posts" :key="post.id" class="mine-post">
              <div class="mp-title">{{ post.title }}
                <el-tag v-if="post.recipeName" size="small" type="primary" style="margin-left: 8px">
                  {{ post.recipeName }}
                </el-tag>
              </div>
              <div class="mp-content">{{ post.content }}</div>
              <div class="mp-meta">
                <span>{{ formatTime(post.publishTime) }}</span>
                <span>赞 {{ post.likeCount }} · 评论 {{ post.commentCount }}</span>
              </div>
            </div>
            <el-pagination v-if="page.total > page.size" v-model:current-page="page.current"
              :page-size="page.size" :total="page.total" layout="prev, pager, next"
              @current-change="loadPosts" style="justify-content: center; margin-top: 12px" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { userProfileApi, userPostsApi, followApi } from '@/api/community'

const route = useRoute()
const userId = Number(route.params.userId)

// 用户信息与动态
const profile = ref(null)
const posts = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

onMounted(async () => {
  const res = await userProfileApi(userId)
  profile.value = res.data
  await loadPosts()
})

/**
 * 加载用户动态
 */
async function loadPosts() {
  loading.value = true
  try {
    const res = await userPostsApi(userId, { current: page.current, size: page.size })
    posts.value = res.data.records
    page.total = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

/**
 * 关注切换
 */
async function handleFollow() {
  const res = await followApi(userId)
  profile.value.followed = res.data
  profile.value.followerCount += res.data ? 1 : -1
  ElMessage.success(res.data ? '已关注' : '已取消关注')
}

/** 时间格式化 */
function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.user-head {
  text-align: center;
}

.big-avatar {
  background: linear-gradient(135deg, var(--brand-500), var(--brand-700));
  color: #fff;
  font-size: 28px;
  font-weight: bold;
}

.user-name {
  font-size: 18px;
  font-weight: bold;
  margin-top: 10px;
}

.user-account {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}

.stat-row {
  display: flex;
}

.stat {
  flex: 1;
  text-align: center;
}

.stat b {
  font-size: 18px;
  color: #303133;
  display: block;
}

.stat span {
  font-size: 12px;
  color: #909399;
}

/* 动态项 */
.mine-post {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.mp-title {
  font-weight: bold;
  margin-bottom: 4px;
}

.mp-content {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.mp-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
}
</style>