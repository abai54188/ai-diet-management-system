package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 食谱改良结果: 原版与改良版对比
 *
 * @author diet
 */
@Data
public class ImproveRecipeVO {

    /** 原版菜品(含本地库计算营养) */
    private AiDishVO original;

    /** 改良版菜品(含本地库计算营养) */
    private AiDishVO improved;

    /** 热量差(改良版-原版, 负数为降低) */
    private BigDecimal calorieDiff;

    /** 蛋白质差(g) */
    private BigDecimal proteinDiff;

    /** 碳水差(g) */
    private BigDecimal carbohydrateDiff;

    /** 脂肪差(g) */
    private BigDecimal fatDiff;
}