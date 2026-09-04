package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 每日摄入汇总视图对象(实际摄入 vs 推荐值)
 *
 * @author diet
 */
@Data
public class DailySummaryVO {

    /** 汇总日期 */
    private String date;

    /** 当日实际总热量(kcal) */
    private BigDecimal totalCalorie;

    /** 当日实际蛋白质(g) */
    private BigDecimal totalProtein;

    /** 当日实际碳水(g) */
    private BigDecimal totalCarbohydrate;

    /** 当日实际脂肪(g) */
    private BigDecimal totalFat;

    /** 当日实际钠(mg) */
    private BigDecimal totalSodium;

    /** 当日实际膳食纤维(g) */
    private BigDecimal totalFiber;

    /** 推荐热量(kcal) */
    private BigDecimal recommendCalorie;

    /** 热量差值(实际-推荐, 正为超标) */
    private BigDecimal calorieDiff;

    /** 蛋白差值(g, 正为达标富余) */
    private BigDecimal proteinDiff;

    /** 碳水差值(g) */
    private BigDecimal carbDiff;

    /** 脂肪差值(g) */
    private BigDecimal fatDiff;

    /** 分餐明细 */
    private List<MealSummary> meals;

    /**
     * 单餐汇总
     */
    @Data
    public static class MealSummary {

        /** 用餐时段: BREAKFAST/LUNCH/DINNER/SNACK */
        private String mealType;

        /** 时段中文名 */
        private String mealLabel;

        /** 该餐实际热量(kcal) */
        private BigDecimal calorie;

        /** 该餐目标热量(kcal, 按三餐比例分配; 加餐无目标) */
        private BigDecimal targetCalorie;

        /** 记录条数 */
        private Integer recordCount;
    }
}