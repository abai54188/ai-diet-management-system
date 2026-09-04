<template>
  <div class="square-page">
    <!-- 顶部: 排序Tab + 发布入口 -->
    <div class="square-header">
      <el-radio-group v-model="sort" @change="loadFeed">
        <el-radio-button value="new">最新</el-radio-button>
        <el-radio-button value="hot">热门</el-radio-button>
        <el-radio-button value="follow">关注</el-radio-button>
      </el-radio-group>
      <el-button type="primary" @click="$router.push('/community/publish')">
        <el-icon><EditPen /></el-icon>&nbsp;发布动态
      </el-button>
    </div>

    <!-- 信息流 -->
    <div v-loading="loading" class="feed-list">
      <el-empty v-if="!feedList.length && !loading" :description="sort === 'follow' ? '关注的人还没有发布动态' : '暂无动态'" />
      <el-card v-for="post in feedList" :key="post.id" shadow="hover" class="post-card">
        <!-- 作者行 -->
        <div class="post-author">
          <el-avatar :size="40" class="author-avatar" @click="goProfile(post.authorId)">
            {{ (post.authorNickname || '?').charAt(0) }}
          </el-avatar>
          <div class="author-info">
            <div class="author-name" @click="goProfile(post.authorId)">{{ post.authorNickname }}</div>
            <div class="post-time">{{ formatTime(post.publishTime) }}</div>
          </div>
          <!-- 关注按钮(非本人) -->
          <el-button v-if="post.authorId !== userInfo?.id && post.followed !== null" size="small"
            :type="post.followed ? 'default' : 'primary'" plain @click="handleFollow(post)">
            {{ post.followed ? '已关注' : '+ 关注' }}
          </el-button>
        </div>

        <!-- 内容 -->
        <div class="post-title">{{ post.title }}</div>
        <div class="post-content">{{ post.content }}</div>
        <!-- 图片 -->
        <div v-if="post.images?.length" class="post-images">
          <el-image v-for="(img, i) in post.images" :key="i" :src="img" fit="cover"
            class="post-img" :preview-src-list="post.images" :initial-index="i" preview-teleported />
        </div>
        <!-- 关联食谱 -->
        <div v-if="post.recipeName" class="post-recipe" @click="goRecipe">
          <el-icon><Food /></el-icon> 关联食谱: {{ post.recipeName }}
        </div>

        <!-- 互动栏 -->
        <div class="post-actions">
          <div class="action-item" :class="{ active: post.liked }" @click="handleLike(post)">
            <el-icon><component :is="post.liked ? 'StarFilled' : 'Star'" /></el-icon>
            <span>{{ post.likeCount }}</span>
          </div>
          <div class="action-item" @click="openComments(post)">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ post.commentCount }}</span>
          </div>
          <div class="action-item" @click="openReport(post)">
            <el-icon><Warning /></el-icon>
            <span>举报</span>
          </div>
        </div>

        <!-- 评论区(展开式) -->
        <div v-if="post.showComments" class="comment-area">
          <div v-if="post.commentList?.length" class="comment-list">
            <div v-for="c in post.commentList" :key="c.id" class="comment-item">
              <b>{{ c.nickname }}:</b> {{ c.content }}
            </div>
          </div>
          <div v-else class="comment-empty">暂无评论</div>
          <div class="comment-input">
            <el-input v-model="post.commentText" placeholder="友善评论..." size="small"
              @keyup.enter="submitComment(post)" />
            <el-button type="primary" size="small" :disabled="!post.commentText" @click="submitComment(post)">
              发送
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 分页 -->
    <el-pagination v-model:current-page="page.current" :page-size="page.size" :total="page.total"
      layout="prev, pager, next" class="feed-pagination" @current-change="loadFeed" />

    <!-- 举报弹窗 -->
    <el-dialog v-model="reportDialog.visible" title="举报内容" width="380px">
      <el-input v-model="reportDialog.reason" type="textarea" :rows="3" placeholder="请填写举报理由" />
      <template #footer>
        <el-button @click="reportDialog.visible = false">取消</el-button>
        <el-button type="danger" :disabled="!reportDialog.reason" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { feedApi, likeApi, commentApi, commentsApi, followApi, reportApi } from '@/api/community'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const userInfo = userStore.userInfo

// 信息流状态
const sort = ref('new')
const feedList = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

/**
 * 加载信息流
 */
async function loadFeed() {
  loading.value = true
  try {
    const res = await feedApi({ sort: sort.value, current: page.current, size: page.size })
    feedList.value = res.data.records.map((p) => ({ ...p, showComments: false, commentList: [], commentText: '' }))
    page.total = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

/**
 * 点赞切换
 */
async function handleLike(post) {
  const res = await likeApi(post.id)
  post.liked = res.data
  post.likeCount += res.data ? 1 : -1
}

/**
 * 关注/取关
 */
async function handleFollow(post) {
  const res = await followApi(post.authorId)
  post.followed = res.data
  ElMessage.success(res.data ? '已关注' : '已取消关注')
}

/**
 * 展开评论
 */
async function openComments(post) {
  post.showComments = !post.showComments
  if (post.showComments && !post.commentList.length) {
    const res = await commentsApi(post.id)
    post.commentList = res.data
  }
}

/**
 * 提交评论
 */
async function submitComment(post) {
  await commentApi({ postId: post.id, content: post.commentText })
  post.commentText = ''
  post.commentCount++
  const res = await commentsApi(post.id)
  post.commentList = res.data
  ElMessage.success('评论成功')
}

/**
 * 打开举报弹窗
 */
const reportDialog = reactive({ visible: false, postId: null, reason: '' })
function openReport(post) {
  reportDialog.postId = post.id
  reportDialog.reason = ''
  reportDialog.visible = true
}

/**
 * 提交举报
 */
async function submitReport() {
  await reportApi({ targetType: 'POST', targetId: reportDialog.postId, reason: reportDialog.reason })
  reportDialog.visible = false
  ElMessage.success('举报已提交，管理员将尽快处理')
}

/** 跳转个人主页 */
function goProfile(userId) {
  router.push(`/community/profile/${userId}`)
}

/** 跳转食谱 */
function goRecipe() {
  router.push('/recipe/generate')
}

/** 时间格式化 */
function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(loadFeed)
</script>

<style scoped>
.square-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.feed-list {
  min-height: 200px;
}

.post-card {
  margin-bottom: 14px;
}

/* 作者行 */
.post-author {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.author-avatar {
  background: linear-gradient(135deg, var(--brand-500), var(--brand-700));
  color: #fff;
  cursor: pointer;
  font-weight: bold;
}

.author-name {
  font-weight: bold;
  cursor: pointer;
}

.post-time {
  font-size: 12px;
  color: #909399;
}

.author-info {
  flex: 1;
}

/* 内容 */
.post-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 6px;
}

.post-content {
  color: #606266;
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 8px;
  white-space: pre-wrap;
}

/* 图片九宫格 */
.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.post-img {
  width: 160px;
  height: 120px;
  border-radius: 6px;
  cursor: pointer;
}

.post-recipe {
  font-size: 13px;
  color: var(--brand-500);
  background: #ecf5ff;
  padding: 6px 10px;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;
}

/* 互动栏 */
.post-actions {
  display: flex;
  gap: 28px;
  border-top: 1px solid #f0f0f0;
  padding-top: 10px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  cursor: pointer;
  font-size: 14px;
}

.action-item.active {
  color: var(--data-fat);
}

.action-item:hover {
  color: var(--brand-500);
}

/* 评论区 */
.comment-area {
  background: #fafafa;
  border-radius: 6px;
  padding: 10px;
  margin-top: 10px;
}

.comment-item {
  font-size: 13px;
  color: #606266;
  padding: 4px 0;
  border-bottom: 1px dashed #ebeef5;
}

.comment-empty {
  font-size: 13px;
  color: #c0c4cc;
  text-align: center;
  padding: 6px;
}

.comment-input {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.feed-pagination {
  justify-content: center;
  margin-top: 10px;
}
</style>