import request from '@/utils/request'

/**
 * 社区模块接口
 */
// 发布动态(文字+图片, 敏感词自动过滤)
export const publishApi = (data) => request.post('/community/publish', data)

// 信息流: sort=hot/new/follow
export const feedApi = (params) => request.get('/community/feed', { params })

// 指定用户动态
export const userPostsApi = (userId, params) => request.get(`/community/user/${userId}/posts`, { params })

// 个人主页信息
export const userProfileApi = (userId) => request.get(`/community/user/${userId}/profile`)

// 点赞切换
export const likeApi = (postId) => request.post(`/community/${postId}/like`)

// 发表评论
export const commentApi = (data) => request.post('/community/comment', data)

// 评论列表
export const commentsApi = (postId) => request.get(`/community/${postId}/comments`)

// 关注切换
export const followApi = (userId) => request.post(`/community/follow/${userId}`)

// 食谱收藏切换
export const collectRecipeApi = (recipeId, category) =>
  request.post(`/community/recipe/${recipeId}/collect`, null, { params: { category } })

// 收藏夹(分类分组)
export const folderApi = () => request.get('/community/recipe/folder')

// 食谱评分
export const rateRecipeApi = (data) => request.post('/community/recipe/rating', data)

// 食谱评分查询
export const recipeScoreApi = (recipeId) => request.get(`/community/recipe/${recipeId}/score`)

// 举报
export const reportApi = (data) => request.post('/community/report', data)

/**
 * 数据看板接口
 */
export const calorieTrendApi = (days) => request.get('/stats/calorie-trend', { params: { days } })
export const macroRatioApi = (days) => request.get('/stats/macro-ratio', { params: { days } })
export const foodCategoryApi = (days) => request.get('/stats/food-category', { params: { days } })
export const mealPeriodApi = (days) => request.get('/stats/meal-period', { params: { days } })
export const achievementApi = (days) => request.get('/stats/achievement', { params: { days } })

/**
 * 管理后台接口(/api/admin前缀已由后端做角色校验)
 */
export const adminUserListApi = (params) => request.get('/admin/user/list', { params })
export const adminUserStatusApi = (id, status) => request.put(`/admin/user/${id}/status`, null, { params: { status } })
export const adminUserRoleApi = (id, role) => request.put(`/admin/user/${id}/role`, null, { params: { role } })
export const adminPostsApi = (params) => request.get('/admin/content/posts', { params })
export const adminCommentsApi = (params) => request.get('/admin/content/comments', { params })
export const adminCommentStatusApi = (id, status) => request.put(`/admin/content/comment/${id}/status`, null, { params: { status } })
export const adminReportsApi = (params) => request.get('/admin/content/reports', { params })
export const adminHandleReportApi = (id, blockTarget, result) =>
  request.post(`/admin/content/report/${id}/handle`, null, { params: { blockTarget, result } })
export const adminFoodPageApi = (params) => request.get('/admin/food/page', { params })
export const adminFoodSaveApi = (data) => request.post('/admin/food', data)
export const adminFoodUpdateApi = (data) => request.put('/admin/food', data)
export const adminFoodDeleteApi = (id) => request.delete(`/admin/food/${id}`)
export const adminOverviewApi = () => request.get('/admin/stats/overview')

/**
 * 食谱列表(社区分享选择用)
 */
export const recipePageApi = (params) => request.get('/recipe/page', { params })