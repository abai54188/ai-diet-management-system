import request from '@/utils/request'

/**
 * 个性化饮食健康管理模块接口
 */
// 计算并保存健康档案(Mifflin-St Jeor)
export const healthCalcApi = () => request.post('/health/calc')

// 查询当前档案计算结果
export const healthProfileApi = () => request.get('/health/profile')

// 自定义三餐比例 {breakfast, lunch, dinner}
export const setMealRatioApi = (data) => request.post('/health/meal-ratio', data)

// 每日摄入汇总 ?date=yyyy-MM-dd
export const dailySummaryApi = (date) => request.get('/health/daily-summary', { params: { date } })

// 每日AI饮食建议 ?date=
export const dailyAdviceApi = (date) => request.get('/health/daily-advice', { params: { date } })

// 阶段性风险分析 ?days=7/30
export const riskAnalysisApi = (days) => request.get('/health/risk', { params: { days } })

// 体重趋势与达标预测 ?days=
export const weightTrendApi = (days) => request.get('/weight/trend', { params: { days } })

// 添加饮食记录
export const dietSaveApi = (data) => request.post('/diet', data)

// 删除饮食记录
export const dietDeleteApi = (id) => request.delete(`/diet/${id}`)

// 分页查询饮食记录
export const dietPageApi = (params) => request.get('/diet/page', { params })

// 保存体重记录
export const weightSaveApi = (data) => request.post('/weight', data)

// 更新用户信息(含目标体重)
export const updateProfileApi = (data) => request.put('/auth/profile', data)

// AI解析食物营养(输入菜名, 解析食材并本地计算营养)
export const aiAnalyzeFoodApi = (data) => request.post('/diet/ai-analyze', data)

// AI拍照识别食物营养(上传照片FormData, 视觉模型识别菜品+估算重量)
export const aiPhotoAnalyzeApi = (formData) => request.post('/diet/ai-photo-analyze', formData)

// AI问答
export const chatSendApi = (question) => request.post('/chat/send', { question })

// 问答历史
export const chatHistoryApi = () => request.get('/chat/history')