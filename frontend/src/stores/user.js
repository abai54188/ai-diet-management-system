import { defineStore } from 'pinia'
import { loginApi, getUserInfoApi } from '@/api/auth'

/**
 * 用户状态管理: 令牌与用户信息
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    // 从 localStorage 恢复登录态，保证刷新后不丢失
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),

  actions: {
    /**
     * 登录: 调用登录接口并持久化令牌与用户信息
     */
    async login(loginForm) {
      const res = await loginApi(loginForm)
      this.token = res.data.token
      this.userInfo = res.data.userInfo
      localStorage.setItem('token', this.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },

    /**
     * 刷新当前用户信息(令牌续期场景下重新拉取)
     */
    async refreshUserInfo() {
      const res = await getUserInfoApi()
      this.userInfo = res.data
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },

    /**
     * 退出登录: 清空本地登录态
     */
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})