package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI食物营养解析结果
 * AI仅输出食材名称+重量，营养数值由后端基于本地营养库计算
 *
 * @author diet
 */
@Data
public class AiFoodParseVO {

    /** 规范化后的食物名称 */
    private String foodName;

    /** 解析出的食材列表(含逐项营养) */
    private List<AiIngredientVO> ingredients;

    /** 总热量kcal(本地库计算) */
    private BigDecimal totalCalorie;

    /** 总蛋白质g(本地库计算) */
    private BigDecimal totalProtein;

    /** 总碳水g(本地库计算) */
    private BigDecimal totalCarbohydrate;

    /** 总脂肪g(本地库计算) */
    private BigDecimal totalFat;

    /** 未匹配到本地库的食材数量 */
    private Integer unmatchedCount;
}