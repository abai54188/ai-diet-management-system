import request from '@/utils/request'

/**
 * AI智能食谱模块接口
 */
// 食材匹配生成食谱(3-5套, 支持忌口/口味/菜系/健康目标约束)
export const generateRecipeApi = (data) => request.post('/recipe/ai/generate', data)

// 收藏AI生成的菜品(切换, 落库为用户食谱并写入收藏夹)
export const collectAiDishApi = (dish, category) =>
  request.post('/recipe/ai/collect', dish, { params: { category } })

// 分析单道菜品: AI还原食材组成+详细做法, 热量/营养由本地库计算
export const analyzeDishApi = (dishName) => request.post('/recipe/ai/analyze-dish', { dishName })

// 生成一周食谱规划(周一到周日三餐, 含每日营养汇总)
export const weekPlanApi = (data) => request.post('/recipe/ai/week-plan', data)

// 替换单餐菜品
export const replaceDishApi = (data) => request.post('/recipe/ai/replace-dish', data)

// 食谱改良(对比两版营养差异)
export const improveRecipeApi = (data) => request.post('/recipe/ai/improve', data)

// 购物清单汇总(跨菜品聚合, 按蔬菜/肉类水产/调料分组)
export const shoppingListApi = (data) => request.post('/recipe/shopping-list', data)