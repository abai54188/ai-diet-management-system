package com.diet.service;

import com.diet.dto.AiDishVO;
import com.diet.dto.AnalyzeDishDTO;
import com.diet.dto.GenerateRecipeDTO;
import com.diet.dto.ImproveRecipeDTO;
import com.diet.dto.ImproveRecipeVO;
import com.diet.dto.RecipeMenuVO;
import com.diet.dto.ReplaceDishDTO;
import com.diet.dto.ShoppingListDTO;
import com.diet.dto.ShoppingListVO;
import com.diet.dto.WeekPlanDTO;
import com.diet.dto.WeekPlanVO;

import java.util.List;

/**
 * AI智能食谱服务接口
 * 设计约束: AI仅负责生成食材名称+重量与做法，所有营养数值由本地营养库计算
 *
 * @author diet
 */
public interface AiRecipeService {

    /**
     * 食材匹配生成食谱(多套菜单, 每套含多道菜品, 做法详细完整)
     *
     * @param dto 食材列表+忌口/口味/菜系/健康目标约束
     * @return 菜单列表(每道菜品含本地库计算的营养值)
     */
    List<RecipeMenuVO> generateRecipes(GenerateRecipeDTO dto);

    /**
     * 分析单道菜品: AI还原食材组成与详细做法, 热量/营养由本地库计算
     *
     * @param dto 菜品名称
     * @return 单道菜品(含做法与营养)
     */
    AiDishVO analyzeDish(AnalyzeDishDTO dto);

    /**
     * 生成一周食谱规划(周一到周日三餐)
     *
     * @param dto 健康目标+用餐人数
     * @return 一周计划(含每日营养汇总)
     */
    WeekPlanVO generateWeekPlan(WeekPlanDTO dto);

    /**
     * 替换单餐菜品并返回新菜品(营养本地计算)
     *
     * @param dto 替换场景(时段/日期/排除列表等)
     * @return 新菜品
     */
    AiDishVO replaceDish(ReplaceDishDTO dto);

    /**
     * 购物清单汇总: 跨菜品聚合食材重量并按蔬菜/肉类水产/调料分组
     *
     * @param dto 选中的菜品列表
     * @return 分类购物清单
     */
    ShoppingListVO buildShoppingList(ShoppingListDTO dto);

    /**
     * 食谱改良: 按方向生成改良版并对比两版营养差异
     *
     * @param dto 原菜品+改良方向
     * @return 原版/改良版/营养差
     */
    ImproveRecipeVO improveRecipe(ImproveRecipeDTO dto);
}