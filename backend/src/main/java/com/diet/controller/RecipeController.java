package com.diet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.BusinessException;
import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.dto.AiDishVO;
import com.diet.entity.Recipe;
import com.diet.entity.RecipeCollect;
import com.diet.mapper.RecipeCollectMapper;
import com.diet.service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 食谱控制器
 * 支持分页、名称模糊搜索、难度/官方筛选；用户仅能修改/删除自己创建的食谱
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/recipe")
public class RecipeController {

    private final RecipeService recipeService;

    private final RecipeCollectMapper recipeCollectMapper;

    private final ObjectMapper objectMapper;

    public RecipeController(RecipeService recipeService, RecipeCollectMapper recipeCollectMapper,
                            ObjectMapper objectMapper) {
        this.recipeService = recipeService;
        this.recipeCollectMapper = recipeCollectMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询食谱列表
     *
     * @param current     当前页码
     * @param size        每页条数
     * @param keyword     食谱名称关键字(模糊匹配)
     * @param difficulty  难度: 1-简单, 2-一般, 3-较难
     * @param isOfficial  是否官方: 0-用户创建, 1-官方
     */
    @GetMapping("/page")
    public Result<IPage<Recipe>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Integer difficulty,
                                       @RequestParam(required = false) Integer isOfficial) {
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<Recipe>()
                .like(StringUtils.hasText(keyword), Recipe::getRecipeName, keyword)
                .eq(difficulty != null, Recipe::getDifficulty, difficulty)
                .eq(isOfficial != null, Recipe::getIsOfficial, isOfficial)
                .orderByDesc(Recipe::getCreateTime);
        return Result.success(recipeService.page(new Page<>(current, size), wrapper));
    }

    /**
     * 查询食谱详情
     */
    @GetMapping("/{id}")
    public Result<Recipe> detail(@PathVariable Long id) {
        Recipe recipe = recipeService.getById(id);
        if (recipe == null) {
            throw new BusinessException("食谱不存在");
        }
        return Result.success(recipe);
    }

    /**
     * 创建食谱(自动标记为用户创建，非官方)
     */
    @PostMapping
    public Result<Void> save(@RequestBody Recipe recipe) {
        recipe.setId(null);
        recipe.setUserId(UserContext.get().getUserId());
        recipe.setIsOfficial(0);
        recipeService.save(recipe);
        return Result.success();
    }

    /**
     * 修改食谱(仅允许修改本人创建的食谱)
     */
    @PutMapping
    public Result<Void> update(@RequestBody Recipe recipe) {
        if (recipe.getId() == null) {
            throw new BusinessException("食谱ID不能为空");
        }
        boolean updated = recipeService.lambdaUpdate()
                .eq(Recipe::getId, recipe.getId())
                .eq(Recipe::getUserId, UserContext.get().getUserId())
                .update(recipe);
        if (!updated) {
            throw new BusinessException("食谱不存在或无权修改");
        }
        return Result.success();
    }

    /**
     * 删除食谱(仅允许删除本人创建的食谱)
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean removed = recipeService.lambdaUpdate()
                .eq(Recipe::getId, id)
                .eq(Recipe::getUserId, UserContext.get().getUserId())
                .remove();
        if (!removed) {
            throw new BusinessException("食谱不存在或无权删除");
        }
        return Result.success();
    }

    /**
     * 收藏AI生成的菜品(切换):
     * 首次收藏会将菜品落库为用户食谱并写入收藏夹；再次调用则取消收藏
     * 收藏后可到「食谱收藏夹」页查看
     *
     * @param dish     AI生成的单道菜品
     * @param category 收藏分类(可选, 默认"AI生成食谱")
     */
    @PostMapping("/ai/collect")
    public Result<Map<String, Object>> collectAiDish(@RequestBody AiDishVO dish,
                                                     @RequestParam(required = false) String category) {
        if (dish == null || !StringUtils.hasText(dish.getDishName())) {
            throw new BusinessException("菜品数据无效");
        }
        Long userId = UserContext.get().getUserId();
        // 按(用户, 菜名)复用已落库的食谱, 避免重复创建
        Recipe recipe = recipeService.lambdaQuery()
                .eq(Recipe::getUserId, userId)
                .eq(Recipe::getRecipeName, dish.getDishName())
                .one();
        if (recipe == null) {
            recipe = new Recipe();
            recipe.setRecipeName(dish.getDishName());
            recipe.setCookingMethod(dish.getSteps() == null ? ""
                    : String.join("\n", dish.getSteps()));
            recipe.setIngredients(toIngredientsJson(dish));
            recipe.setCaloriePerServing(dish.getCalorie());
            recipe.setDifficulty(dish.getDifficulty());
            recipe.setCookingTime(dish.getCookingTime());
            recipe.setUserId(userId);
            recipe.setIsOfficial(0);
            recipeService.save(recipe);
        }
        // 收藏切换
        RecipeCollect exist = recipeCollectMapper.selectOne(new LambdaQueryWrapper<RecipeCollect>()
                .eq(RecipeCollect::getUserId, userId)
                .eq(RecipeCollect::getRecipeId, recipe.getId()));
        boolean collected;
        if (exist != null) {
            recipeCollectMapper.deleteById(exist.getId());
            collected = false;
        } else {
            RecipeCollect collect = new RecipeCollect();
            collect.setUserId(userId);
            collect.setRecipeId(recipe.getId());
            collect.setCategory(StringUtils.hasText(category) ? category : "AI生成食谱");
            recipeCollectMapper.insert(collect);
            collected = true;
        }
        return Result.success(Map.of("collected", collected, "recipeId", recipe.getId()));
    }

    /**
     * 将菜品食材列表序列化为JSON字符串([{foodName,weight}])
     */
    private String toIngredientsJson(AiDishVO dish) {
        try {
            if (dish.getIngredients() == null) {
                return "[]";
            }
            List<Map<String, Object>> list = dish.getIngredients().stream().map(i -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("foodName", i.getFoodName());
                m.put("weight", i.getWeight());
                return m;
            }).toList();
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new BusinessException("食材数据格式错误");
        }
    }
}