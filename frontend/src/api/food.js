import request from '@/utils/request'

/**
 * 饮食营养查询工具相关接口
 */

/**
 * 分页搜索食材(名称/别名模糊 + 分类筛选)
 * @param {Object} params - { current, size, keyword, category }
 */
export const foodPageApi = (params) => request.get('/food/page', { params })

/**
 * 输入联想: 返回前5条匹配食材
 * @param {String} keyword - 用户输入关键词
 */
export const foodSuggestApi = (keyword) => request.get('/food/suggest', { params: { keyword } })

/**
 * 批量计算营养摄入: 传入 [{foodId, weight}] 列表
 * 返回每项热量/蛋白/碳水/脂肪 + 三大营养素汇总
 * @param {Array} items - 食材计算项列表
 */
export const foodCalculateApi = (items) => request.post('/food/calculate', { items })

/**
 * 食材详情: 返回完整营养成分表
 * @param {Number} id - 食材ID
 */
export const foodDetailApi = (id) => request.get(`/food/${id}`)

/**
 * 查询当前用户收藏列表(关联食材营养信息)
 */
export const collectListApi = () => request.get('/collect/list')

/**
 * 新增收藏(幂等，重复收藏自动忽略)
 * @param {Object} data - { foodId, fixedWeight }
 */
export const collectSaveApi = (data) => request.post('/collect', data)

/**
 * 修改收藏固定重量
 * @param {Object} data - { id, fixedWeight }
 */
export const collectUpdateApi = (data) => request.put('/collect', data)

/**
 * 删除收藏
 * @param {Number} id - 收藏ID
 */
export const collectDeleteApi = (id) => request.delete(`/collect/${id}`)