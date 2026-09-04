import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

/**
 * axios 实例封装
 * - baseURL 走 Vite 代理，转发到后端 8080
 * - 请求拦截器自动携带 JWT 令牌
 * - 响应拦截器统一处理后端返回的 Result 结构与错误
 */
const service = axios.create({
  baseURL: '/api',
  // 超时放宽到 120 秒: 大模型接口(识菜/问答/生成食谱)耗时长, 10秒会提前中断诱发"网络异常"
  timeout: 120000
})

// 请求拦截器: 自动携带令牌
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器: 统一处理业务码与异常
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端约定 code=200 为成功
    if (res.code !== 200) {
      // 令牌失效: 清空登录态并跳转登录页
      if (res.code === 401) {
        handleUnauthorized()
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    // HTTP 层错误(网络异常/超时/服务器不可达)
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        handleUnauthorized()
      } else if (status === 403) {
        ElMessage.error('无权限访问该资源')
      } else {
        ElMessage.error('服务器异常，请稍后重试')
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

/**
 * 未授权统一处理: 退出登录并携带当前路径跳转
 */
function handleUnauthorized() {
  const userStore = useUserStore()
  userStore.logout()
  ElMessage.error('登录已过期，请重新登录')
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
}

export default service