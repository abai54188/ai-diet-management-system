package com.diet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI菜品食材项(AI仅输出名称+重量，营养值由后端本地库计算填充)
 *
 * @author diet
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiIngredientVO {

    /** 食材名称(AI输出，国内通用叫法) */
    private String foodName;

    /** 重量(g, 可食部生重) */
    private BigDecimal weight;

    /** 匹配到的本地食材ID(未匹配为null) */
    private Long foodId;

    /** 是否成功匹配本地营养库 */
    private Boolean matched;

    /** 本地库分类 */
    private String category;

    /** 该项热量kcal(后端计算) */
    private BigDecimal calorie;

    /** 该项蛋白质g(后端计算) */
    private BigDecimal protein;

    /** 该项碳水化合物g(后端计算) */
    private BigDecimal carbohydrate;

    /** 该项脂肪g(后端计算) */
    private BigDecimal fat;
}