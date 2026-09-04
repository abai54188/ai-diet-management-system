<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="tab-header">
          <span>内容管理</span>
          <el-radio-group v-model="tab">
            <el-radio-button value="posts">动态</el-radio-button>
            <el-radio-button value="comments">评论</el-radio-button>
            <el-radio-button value="reports">举报处理</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 动态Tab -->
      <template v-if="tab === 'posts'">
        <el-table :data="posts" v-loading="loading" size="small">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
          <el-table-column prop="authorName" label="作者" width="110" />
          <el-table-column prop="likeCount" label="赞" width="60" align="center" />
          <el-table-column prop="commentCount" label="评论" width="60" align="center" />
          <el-table-column label="被举报" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.reported" type="danger" size="small">{{ row.reported }}次</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="publishTime" label="发布时间" width="170" />
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-button type="danger" link size="small" @click="deletePost(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="postPage.current" :page-size="postPage.size" :total="postPage.total"
          layout="total, prev, pager, next" class="table-pagination" @current-change="loadPosts" />
      </template>

      <!-- 评论Tab -->
      <template v-else-if="tab === 'comments'">
        <el-table :data="comments" v-loading="loading" size="small">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="postId" label="动态ID" width="80" />
          <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="authorName" label="评论人" width="110" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">
                {{ row.status === 1 ? '已屏蔽' : '正常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="170" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button :type="row.status === 1 ? 'success' : 'warning'" link size="small" @click="toggleComment(row)">
                {{ row.status === 1 ? '恢复' : '屏蔽' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="commentPage.current" :page-size="commentPage.size" :total="commentPage.total"
          layout="total, prev, pager, next" class="table-pagination" @current-change="loadComments" />
      </template>

      <!-- 举报处理Tab -->
      <template v-else>
        <el-table :data="reports" v-loading="loading" size="small">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column label="目标" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="row.targetType === 'POST' ? 'primary' : 'warning'">
                {{ row.targetType === 'POST' ? '动态#' + row.targetId : '评论#' + row.targetId }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetPreview" label="内容预览" min-width="140" show-overflow-tooltip />
          <el-table-column prop="reason" label="举报理由" min-width="120" show-overflow-tooltip />
          <el-table-column prop="reporterName" label="举报人" width="100" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '已处理' : '待处理' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" align="center">
            <template #default="{ row }">
              <template v-if="row.status === 0">
                <el-button type="danger" size="small" @click="handleReport(row, true)">屏蔽并结案</el-button>
                <el-button type="info" size="small" plain @click="handleReport(row, false)">仅结案</el-button>
              </template>
              <span v-else>{{ row.handleResult }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="reportPage.current" :page-size="reportPage.size" :total="reportPage.total"
          layout="total, prev, pager, next" class="table-pagination" @current-change="loadReports" />
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { adminPostsApi, adminCommentsApi, adminCommentStatusApi, adminReportsApi, adminHandleReportApi } from '@/api/community'

const tab = ref('posts')
const loading = ref(false)

// 三个Tab的数据与分页
const posts = ref([])
const postPage = reactive({ current: 1, size: 10, total: 0 })
const comments = ref([])
const commentPage = reactive({ current: 1, size: 10, total: 0 })
const reports = ref([])
const reportPage = reactive({ current: 1, size: 10, total: 0 })

// Tab切换时懒加载
watch(tab, (t) => {
  if (t === 'posts' && !posts.value.length) loadPosts()
  if (t === 'comments' && !comments.value.length) loadComments()
  if (t === 'reports' && !reports.value.length) loadReports()
})

onMounted(loadPosts)

/**
 * 加载动态列表
 */
async function loadPosts() {
  loading.value = true
  try {
    const res = await adminPostsApi({ current: postPage.current, size: postPage.size })
    posts.value = res.data.records
    postPage.total = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

/**
 * 删除动态(管理端逻辑删除)
 */
async function deletePost(row) {
  await ElMessageBox.confirm(`确定删除动态「${row.title}」吗？`, '内容删除', { type: 'warning' })
  await request.delete(`/admin/content/post/${row.id}`)
  ElMessage.success('已删除')
  await loadPosts()
}

/**
 * 加载评论列表
 */
async function loadComments() {
  loading.value = true
  try {
    const res = await adminCommentsApi({ current: commentPage.current, size: commentPage.size })
    comments.value = res.data.records
    commentPage.total = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

/**
 * 屏蔽/恢复评论
 */
async function toggleComment(row) {
  await adminCommentStatusApi(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已恢复' : '已屏蔽')
  await loadComments()
}

/**
 * 加载举报列表
 */
async function loadReports() {
  loading.value = true
  try {
    const res = await adminReportsApi({ current: reportPage.current, size: reportPage.size })
    reports.value = res.data.records
    reportPage.total = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

/**
 * 处理举报
 */
async function handleReport(row, block) {
  if (block) {
    await ElMessageBox.confirm('将屏蔽被举报内容并结案，确定吗？', '举报处理', { type: 'warning' })
  }
  await adminHandleReportApi(row.id, block ? 1 : 0, block ? '已屏蔽违规内容' : '核实无违规')
  ElMessage.success('已处理')
  await loadReports()
}
</script>

<style scoped>
.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>