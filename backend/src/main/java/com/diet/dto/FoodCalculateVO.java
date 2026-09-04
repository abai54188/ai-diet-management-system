package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 热量计算结果视图对象
 * 按传入重量换算后的每项营养量 + 全部合计
 *
 * @author diet
 */
@Data
public class FoodCalculateVO {

    /** 汇总: 总热量(kcal) */
    private BigDecimal totalCalorie;

    /** 汇总: 总蛋白质(g) */
    private BigDecimal totalProtein;

    /** 汇总: 总碳水化合物(g) */
    private BigDecimal totalCarbohydrate;

    /** 汇总: 总脂肪(g) */
    private BigDecimal totalFat;

    /** 每项食材的明细计算结果 */
    private List<ItemVO> items;

    /**
     * 单项食材计算结果
     */
    @Data
    public static class ItemVO {

        /** 食材ID */
        private Long foodId;

        /** 食材名称 */
        private String foodName;

        /** 分类 */
        private String category;

        /** 重量(g) */
        private BigDecimal weight;

        /** 该项热量(kcal) */
        private BigDecimal calorie;

        /** 该项蛋白质(g) */
        private BigDecimal protein;

        /** 该项碳水化合物(g) */
        private BigDecimal carbohydrate;

        /** 该项脂肪(g) */
        private BigDecimal fat;
    }
}