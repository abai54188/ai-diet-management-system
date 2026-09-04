package com.diet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI生成菜品视图对象
 * dishName/description/difficulty/cookingTime/steps/ingredients由AI输出，
 * calorie/protein/carbohydrate/fat由后端基于本地营养库计算
 *
 * @author diet
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiDishVO {

    /** 菜品名称 */
    private String dishName;

    /** 一句话简介 */
    private String description;

    /** 难度: 1-简单, 2-一般, 3-较难 */
    private Integer difficulty;

    /** 烹饪耗时(分钟) */
    private Integer cookingTime;

    /** 烹饪步骤 */
    private List<String> steps;

    /** 食材列表 */
    private List<AiIngredientVO> ingredients;

    /** 菜品总热量kcal(后端本地库计算) */
    private BigDecimal calorie;

    /** 菜品总蛋白质g(后端本地库计算) */
    private BigDecimal protein;

    /** 菜品总碳水g(后端本地库计算) */
    private BigDecimal carbohydrate;

    /** 菜品总脂肪g(后端本地库计算) */
    private BigDecimal fat;
}