package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.dto.AiDishVO;
import com.diet.dto.AiIngredientVO;
import com.diet.entity.FoodNutrition;
import com.diet.mapper.FoodNutritionMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 营养匹配计算器
 * 职责: 将AI输出的食材名称匹配到本地 food_nutrition 营养库，并按重量换算营养值
 * 硬性约束: 所有热量/营养数值均由本类基于本地库计算，AI不参与数值计算
 *
 * @author diet
 */
@Component
public class NutritionCalculator {

    private final FoodNutritionMapper foodMapper;

    public NutritionCalculator(FoodNutritionMapper foodMapper) {
        this.foodMapper = foodMapper;
    }

    /**
     * 食材名称匹配本地营养库
     * 匹配优先级: 名称精确 > 别名分号段精确 > 名称/别名包含(取名称最短者)
     *
     * @param foodName AI输出的食材名称
     * @return 匹配到的食材，未匹配返回null
     */
    public FoodNutrition match(String foodName) {
        if (!StringUtils.hasText(foodName)) {
            return null;
        }
        String name = foodName.trim();
        // 1. 名称精确匹配
        FoodNutrition exact = foodMapper.selectOne(new LambdaQueryWrapper<FoodNutrition>()
                .eq(FoodNutrition::getFoodName, name)
                .last("LIMIT 1"));
        if (exact != null) {
            return exact;
        }
        // 2. 拉取名称或别名包含该关键词的候选集
        List<FoodNutrition> candidates = foodMapper.selectList(new LambdaQueryWrapper<FoodNutrition>()
                .and(w -> w.like(FoodNutrition::getFoodName, name)
                        .or()
                        .like(FoodNutrition::getAlias, name)));
        if (candidates.isEmpty()) {
            return null;
        }
        // 3. 别名按分号切分后精确等于关键词(如"鸡蛋"命中别名"鸡蛋;鸡卵;鸡子")
        for (FoodNutrition c : candidates) {
            if (c.getAlias() != null && Arrays.stream(c.getAlias().split("[;；]"))
                    .anyMatch(token -> token.trim().equals(name))) {
                return c;
            }
        }
        // 4. 兜底: 取名称最短的包含项(更接近本体，避免命中挂面等衍生物)
        return candidates.stream()
                .min(Comparator.comparingInt(c -> c.getFoodName().length()))
                .orElse(null);
    }

    /**
     * 填充菜品营养: 遍历菜品食材，逐项匹配本地库并按 重量/100*每100g营养值 换算，
     * 最后汇总菜品级热量与三大营养素
     *
     * @param dish 菜品(ingredients中的每项将被填充营养值)
     */
    public void fillDishNutrition(AiDishVO dish) {
        BigDecimal calorie = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carbohydrate = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        if (dish.getIngredients() != null) {
            for (AiIngredientVO ing : dish.getIngredients()) {
                if (ing.getWeight() == null) {
                    ing.setWeight(BigDecimal.ZERO);
                }
                FoodNutrition food = match(ing.getFoodName());
                if (food == null) {
                    // 未匹配: 标记后由前端提示，营养按0计
                    ing.setMatched(false);
                    continue;
                }
                // 换算系数 = 重量(g) / 100
                BigDecimal factor = ing.getWeight()
                        .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                ing.setFoodId(food.getId());
                ing.setMatched(true);
                ing.setCategory(food.getCategory());
                ing.setCalorie(multiply(food.getCalorie(), factor));
                ing.setProtein(multiply(food.getProtein(), factor));
                ing.setCarbohydrate(multiply(food.getCarbohydrate(), factor));
                ing.setFat(multiply(food.getFat(), factor));
                // 累加到菜品汇总
                calorie = calorie.add(ing.getCalorie());
                protein = protein.add(ing.getProtein());
                carbohydrate = carbohydrate.add(ing.getCarbohydrate());
                fat = fat.add(ing.getFat());
            }
        }
        dish.setCalorie(calorie.setScale(1, RoundingMode.HALF_UP));
        dish.setProtein(protein.setScale(1, RoundingMode.HALF_UP));
        dish.setCarbohydrate(carbohydrate.setScale(1, RoundingMode.HALF_UP));
        dish.setFat(fat.setScale(1, RoundingMode.HALF_UP));
    }

    /**
     * 营养值 * 系数，保留1位小数
     */
    private BigDecimal multiply(BigDecimal base, BigDecimal factor) {
        return base.multiply(factor).setScale(1, RoundingMode.HALF_UP);
    }
}