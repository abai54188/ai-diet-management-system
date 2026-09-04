import { defineStore } from 'pinia'

/**
 * AI食谱模块共享状态
 * 跨Tab共享: 生成的食谱、购物清单选中菜品、周计划、改良目标
 */
export const useRecipeStore = defineStore('recipe', {
  state: () => ({
    // 最近一次生成的食谱列表
    generatedDishes: [],
    // 已加入购物清单的菜品
    shoppingDishes: [],
    // 一周食谱计划
    weekPlan: null,
    // 食谱改良目标菜品
    improveTarget: null
  }),
  actions: {
    /** 设置生成结果 */
    setGenerated(dishes) {
      this.generatedDishes = dishes || []
    },
    /** 切换菜品加入/移出购物清单(按菜品名去重) */
    toggleShopping(dish) {
      const idx = this.shoppingDishes.findIndex((d) => d.dishName === dish.dishName)
      if (idx >= 0) {
        this.shoppingDishes.splice(idx, 1)
        return false
      }
      this.shoppingDishes.push(dish)
      return true
    },
    /** 批量加入购物清单(去重) */
    addDishesToShopping(dishes) {
      let added = 0
      dishes.forEach((d) => {
        if (d && !this.shoppingDishes.some((s) => s.dishName === d.dishName)) {
          this.shoppingDishes.push(d)
          added++
        }
      })
      return added
    },
    /** 清空购物清单 */
    clearShopping() {
      this.shoppingDishes = []
    },
    /** 设置改良目标 */
    setImproveTarget(dish) {
      this.improveTarget = dish
    }
  }
})