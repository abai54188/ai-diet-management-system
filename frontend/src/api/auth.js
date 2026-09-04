import request from '@/utils/request'

/**
 * 认证相关接口
 */
// 用户登录
export const loginApi = (data) => request.post('/auth/login', data)

// 用户注册
export const registerApi = (data) => request.post('/auth/register', data)

// 获取当前登录用户信息
export const getUserInfoApi = () => request.get('/auth/info')