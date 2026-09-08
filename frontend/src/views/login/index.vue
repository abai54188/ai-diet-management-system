<template>
  <div class="login-page">
    <!-- 漂浮叶片装饰 (CSS 生成, 缓慢摇曳) -->
    <svg class="leaf leaf-a" viewBox="0 0 24 24" fill="none">
      <path d="M12 2C7 6 4 10 4 14a8 8 0 0 0 16 0c0-4-3-8-8-12z" fill="var(--brand-200)" opacity="0.5" />
    </svg>
    <svg class="leaf leaf-b" viewBox="0 0 24 24" fill="none">
      <path d="M4 20C4 12 10 4 20 4c0 10-8 16-16 16z" fill="var(--brand-100)" opacity="0.8" />
    </svg>
    <svg class="leaf leaf-c" viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="12" r="9" stroke="var(--data-fat)" stroke-width="1.2" opacity="0.35" />
    </svg>

    <!-- 分屏登录卡片 -->
    <div class="login-card page-enter">
      <!-- 左侧: 品牌视觉面板 -->
      <div class="brand-panel">
        <img src="/img/login-hero.jpg" alt="新鲜食材" class="brand-img" />
        <div class="brand-overlay">
          <div class="brand-mark">
            <svg viewBox="0 0 24 24" fill="none" class="mark-icon">
              <path d="M12 21c-4.5-2-8-5.5-8-10 5.5-1.5 9 .5 11 3 2-2.5 5.5-4.5 11-3 0 4.5-3.5 8-8 10-1 .4-1 .4-2 0z" fill="#fff" opacity="0.95" />
              <path d="M12 21c0-4 1.5-7.5 4-10" stroke="var(--brand-600)" stroke-width="1.1" stroke-linecap="round" />
            </svg>
          </div>
          <h1 class="brand-name">食在 AI</h1>
          <p class="brand-slogan">科学饮食 · 健康生活</p>
          <div class="brand-points">
            <span>1010 种国标食材营养库</span>
            <span>Mifflin-St Jeor 精准代谢计算</span>
            <span>AI 食谱 · 社区分享 · 健康看板</span>
          </div>
        </div>
      </div>

      <!-- 右侧: 表单面板 -->
      <div class="form-panel">
        <div class="form-head">
          <h2 class="form-title">AI 智能饮食管理系统</h2>
          <p class="form-sub">登录或创建账号，开启你的健康饮食之旅</p>
        </div>

        <el-tabs v-model="activeTab" stretch>
          <el-tab-pane label="账号登录" name="login">
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" size="large">
              <el-form-item prop="username">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password
                  @keyup.enter="handleLogin" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">登 录</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册账号" name="register">
            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large">
              <el-form-item prop="username">
                <el-input v-model="registerForm.username" placeholder="用户名(4-20位字母数字)" :prefix-icon="User" clearable />
              </el-form-item>
              <el-form-item prop="nickname">
                <el-input v-model="registerForm.nickname" placeholder="请输入昵称" :prefix-icon="Postcard" clearable />
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="registerForm.password" type="password" placeholder="密码(6-20位)" :prefix-icon="Lock" show-password />
              </el-form-item>
              <el-form-item prop="confirmPassword">
                <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" :prefix-icon="Lock" show-password
                  @keyup.enter="handleRegister" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">注 册</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <p class="form-foot">本系统提供的营养数据与建议仅供饮食参考，不构成医疗建议</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard } from '@element-plus/icons-vue'
import { registerApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 当前激活的选项卡
const activeTab = ref('login')
// 提交加载状态
const loading = ref(false)

// 登录表单与校验规则
const loginFormRef = ref(null)
const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 注册表单与校验规则
const registerFormRef = ref(null)
const registerForm = reactive({ username: '', nickname: '', password: '', confirmPassword: '' })
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '用户名须为4-20位字母、数字或下划线', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度须为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        value !== registerForm.password ? callback(new Error('两次输入的密码不一致')) : callback()
      },
      trigger: 'blur'
    }
  ]
}

/**
 * 登录处理: 校验通过后调用接口，成功跳转首页
 */
async function handleLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    // 优先跳转来源页面(路由守卫带来的 redirect 参数)，否则跳首页
    router.push(route.query.redirect || '/home')
  } finally {
    loading.value = false
  }
}

/**
 * 注册处理: 注册成功后自动切换到登录选项卡
 */
async function handleRegister() {
  await registerFormRef.value.validate()
  loading.value = true
  try {
    await registerApi(registerForm)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ---------- 页面底 ---------- */
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  overflow: hidden;
}

/* ---------- 漂浮叶片 (轻微摇曳, 尊重reduced-motion由全局控制) ---------- */
.leaf {
  position: absolute;
  pointer-events: none;
}

.leaf-a {
  width: 140px;
  left: 8%;
  top: 12%;
  transform: rotate(-20deg);
  animation: leaf-sway 7s ease-in-out infinite alternate;
}

.leaf-b {
  width: 100px;
  right: 10%;
  bottom: 16%;
  transform: rotate(30deg);
  animation: leaf-sway 9s ease-in-out infinite alternate-reverse;
}

.leaf-c {
  width: 56px;
  right: 22%;
  top: 18%;
  animation: leaf-sway 11s ease-in-out infinite alternate;
}

@keyframes leaf-sway {
  from {
    transform: translateY(0) rotate(-8deg);
  }
  to {
    transform: translateY(-14px) rotate(8deg);
  }
}

/* ---------- 分屏卡片 ---------- */
.login-card {
  display: flex;
  width: 920px;
  max-width: calc(100vw - 40px);
  min-height: 560px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-pop);
  overflow: hidden;
  z-index: 1;
}

/* 左侧品牌面板 */
.brand-panel {
  position: relative;
  width: 42%;
  flex-shrink: 0;
  overflow: hidden;
}

.brand-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 暗角: 聚焦中心画面, 避免边缘过亮分散注意 */
.brand-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 100% at 50% 42%, transparent 55%, rgba(23, 56, 41, 0.22) 100%);
  pointer-events: none;
}

/* 品牌遮罩: 底部墨绿渐变保证文字可读 (置顶于暗角层之上) */
.brand-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 36px 32px;
  background: linear-gradient(180deg, rgba(23, 56, 41, 0) 30%, rgba(23, 56, 41, 0.62) 62%, rgba(23, 56, 41, 0.92) 100%);
  color: #fff;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}

.mark-icon {
  width: 26px;
  height: 26px;
}

.brand-name {
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 900;
  letter-spacing: 6px;
  margin-bottom: 6px;
}

.brand-slogan {
  font-size: 13px;
  letter-spacing: 3px;
  opacity: 0.85;
  margin-bottom: 18px;
}

/* 信任点列表 */
.brand-points {
  display: flex;
  flex-direction: column;
  gap: 7px;
  font-size: 12.5px;
  opacity: 0.92;
}

.brand-points span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-points span::before {
  content: '';
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--brand-300);
  flex-shrink: 0;
}

/* 右侧表单面板 */
.form-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 44px 48px 28px;
}

.form-head {
  text-align: center;
  margin-bottom: 18px;
}

.form-title {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  color: var(--ink-900);
  letter-spacing: 1px;
}

.form-sub {
  margin-top: 8px;
  color: var(--ink-600);
  font-size: 13px;
}

/* 提交按钮铺满 */
.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 8px;
  border-radius: 10px;
}

/* 底部免责声明 */
.form-foot {
  margin-top: auto;
  padding-top: 18px;
  text-align: center;
  font-size: 12px;
  color: var(--ink-400);
}

/* ---------- 窄屏: 品牌面板收起为顶部横条 ---------- */
@media (max-width: 760px) {
  .login-card {
    flex-direction: column;
    min-height: 0;
  }

  .brand-panel {
    width: 100%;
    height: 160px;
  }

  .brand-points {
    display: none;
  }

  .brand-name {
    font-size: 24px;
    letter-spacing: 4px;
    margin-bottom: 2px;
  }

  .brand-slogan {
    margin-bottom: 0;
  }

  .form-panel {
    padding: 28px 26px 20px;
  }
}
</style>