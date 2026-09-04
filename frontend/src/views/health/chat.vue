<template>
  <div class="chat-page">
    <el-card shadow="never" class="chat-card">
      <template #header>
        <div class="chat-header">
          <span>AI营养助手</span>
          <el-tag type="success" size="small">已关联您的健康档案</el-tag>
        </div>
      </template>

      <!-- 对话区 -->
      <div ref="chatBoxRef" class="chat-box">
        <el-empty v-if="!messages.length" description="向营养助手提问，如：我每天应该吃多少蛋白质？" />
        <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="msg.role === 'USER' ? 'user' : 'ai'">
          <!-- 头像 -->
          <el-avatar :size="34" class="avatar" :class="msg.role === 'USER' ? 'user-avatar' : 'ai-avatar'">
            {{ msg.role === 'USER' ? '我' : 'AI' }}
          </el-avatar>
          <!-- 气泡 -->
          <div class="bubble" :class="msg.role === 'USER' ? 'user-bubble' : 'ai-bubble'">
            <div class="content">{{ msg.content }}</div>
            <div class="time">{{ formatTime(msg.createTime) }}</div>
          </div>
        </div>
        <!-- 加载中 -->
        <div v-if="sending" class="msg-row ai">
          <el-avatar :size="34" class="avatar ai-avatar">AI</el-avatar>
          <div class="bubble ai-bubble">
            <span class="typing">思考中<span class="dots">...</span></span>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <el-input
          v-model="question"
          placeholder="输入营养问题，回车发送（回答将结合您的健康数据）"
          :disabled="sending"
          maxlength="500"
          @keyup.enter="handleSend"
        >
          <template #append>
            <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
          </template>
        </el-input>
        <div class="quick-questions" v-if="!messages.length">
          <el-tag
            v-for="q in quickQuestions"
            :key="q"
            class="quick-tag"
            @click="quickAsk(q)"
          >{{ q }}</el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { chatHistoryApi, chatSendApi } from '@/api/health'

// 消息列表与输入
const messages = ref([])
const question = ref('')
const sending = ref(false)
const chatBoxRef = ref(null)

// 快捷问题
const quickQuestions = ['我每天应该吃多少蛋白质？', '减脂期怎么安排三餐？', '如何增加膳食纤维？', '每天盐吃多少合适？']

/**
 * 页面初始化: 加载历史对话
 */
onMounted(async () => {
  try {
    const res = await chatHistoryApi()
    messages.value = res.data || []
    scrollToBottom()
  } catch (e) { /* 忽略 */ }
})

/**
 * 发送问题
 */
async function handleSend() {
  const q = question.value.trim()
  if (!q || sending.value) return
  sending.value = true
  // 先本地插入用户消息(乐观更新)
  messages.value.push({
    id: 'local-' + Date.now(),
    role: 'USER',
    content: q,
    createTime: new Date().toISOString()
  })
  question.value = ''
  scrollToBottom()
  try {
    const res = await chatSendApi(q)
    messages.value.push(res.data)
    scrollToBottom()
  } catch (e) {
    ElMessage.error('发送失败，请重试')
  } finally {
    sending.value = false
  }
}

/**
 * 快捷提问
 */
function quickAsk(q) {
  question.value = q
  handleSend()
}

/**
 * 滚动到底部
 */
async function scrollToBottom() {
  await nextTick()
  if (chatBoxRef.value) {
    chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight
  }
}

/**
 * 时间格式化
 */
function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.chat-card {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 对话区 */
.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 10px 4px;
  min-height: 300px;
}

.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.msg-row.user {
  flex-direction: row-reverse;
}

.avatar {
  flex-shrink: 0;
  font-weight: bold;
}

.ai-avatar {
  background: linear-gradient(135deg, var(--brand-500), var(--brand-700));
  color: #fff;
}

.user-avatar {
  background: linear-gradient(135deg, var(--brand-500), var(--brand-600));
  color: #fff;
}

/* 气泡 */
.bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 10px;
  position: relative;
}

.ai-bubble {
  background: #f4f4f5;
  border-top-left-radius: 2px;
}

.user-bubble {
  background: linear-gradient(135deg, var(--brand-500), var(--brand-600));
  color: #fff;
  border-top-right-radius: 2px;
}

.content {
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
}

.time {
  font-size: 11px;
  opacity: 0.6;
  margin-top: 4px;
  text-align: right;
}

/* 输入区 */
.input-area {
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.quick-tag {
  cursor: pointer;
}

/* 思考动画 */
.typing {
  color: #909399;
  font-size: 13px;
}

.dots {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 0.2; }
  50% { opacity: 1; }
}
</style>