package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一周食谱规划结果
 * 每日营养由后端基于本地营养库对三餐菜品汇总计算
 *
 * @author diet
 */
@Data
public class WeekPlanVO {

    /** 一周七天计划 */
    private List<DayPlan> days;

    /**
     * 单日计划
     */
    @Data
    public static class DayPlan {

        /** 星期标签(周一~周日) */
        private String dayLabel;

        /** 早餐菜品 */
        private AiDishVO breakfast;

        /** 午餐菜品 */
        private AiDishVO lunch;

        /** 晚餐菜品 */
        private AiDishVO dinner;

        /** 当日总热量kcal(后端计算) */
        private BigDecimal calorie;

        /** 当日总蛋白质g(后端计算) */
        private BigDecimal protein;

        /** 当日总碳水g(后端计算) */
        private BigDecimal carbohydrate;

        /** 当日总脂肪g(后端计算) */
        private BigDecimal fat;
    }
}