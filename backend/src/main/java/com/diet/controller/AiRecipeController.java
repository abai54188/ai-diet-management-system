package com.diet.controller;

import com.diet.common.Result;
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
import com.diet.service.AiRecipeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI智能食谱控制器
 * AI仅生成食材名称+重量与做法，所有营养数值由后端基于本地营养库计算
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/recipe")
public class AiRecipeController {

    private final AiRecipeService aiRecipeService;

    public AiRecipeController(AiRecipeService aiRecipeService) {
        this.aiRecipeService = aiRecipeService;
    }

    /**
     * 食材匹配生成食谱(多套菜单, 每套含多道菜品, 做法详细)
     */
    @PostMapping("/ai/generate")
    public Result<List<RecipeMenuVO>> generate(@Validated @RequestBody GenerateRecipeDTO dto) {
        return Result.success(aiRecipeService.generateRecipes(dto));
    }

    /**
     * 分析单道菜品: AI还原食材组成+详细做法, 热量/营养由本地库计算
     */
    @PostMapping("/ai/analyze-dish")
    public Result<AiDishVO> analyzeDish(@Validated @RequestBody AnalyzeDishDTO dto) {
        return Result.success(aiRecipeService.analyzeDish(dto));
    }

    /**
     * 生成一周食谱规划(周一到周日三餐，含每日营养汇总)
     */
    @PostMapping("/ai/week-plan")
    public Result<WeekPlanVO> weekPlan(@Validated @RequestBody WeekPlanDTO dto) {
        return Result.success(aiRecipeService.generateWeekPlan(dto));
    }

    /**
     * 替换单餐菜品(返回新菜品并本地计算营养)
     */
    @PostMapping("/ai/replace-dish")
    public Result<AiDishVO> replaceDish(@Validated @RequestBody ReplaceDishDTO dto) {
        return Result.success(aiRecipeService.replaceDish(dto));
    }

    /**
     * 购物清单汇总(跨菜品聚合食材重量，按蔬菜/肉类水产/调料分组)
     */
    @PostMapping("/shopping-list")
    public Result<ShoppingListVO> shoppingList(@Validated @RequestBody ShoppingListDTO dto) {
        return Result.success(aiRecipeService.buildShoppingList(dto));
    }

    /**
     * 食谱改良(低卡/低脂/高蛋白/控糖/低盐，对比两版营养差异)
     */
    @PostMapping("/ai/improve")
    public Result<ImproveRecipeVO> improve(@Validated @RequestBody ImproveRecipeDTO dto) {
        return Result.success(aiRecipeService.improveRecipe(dto));
    }
}