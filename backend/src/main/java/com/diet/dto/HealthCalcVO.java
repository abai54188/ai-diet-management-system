package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 健康档案计算结果视图对象
 * Mifflin-St Jeor公式: 男 BMR=10W+6.25H-5A+5 / 女 BMR=10W+6.25H-5A-161
 *
 * @author diet
 */
@Data
public class HealthCalcVO {

    /** 用户信息摘要 */
    private Long userId;

    /** 身高(cm) */
    private BigDecimal height;

    /** 体重(kg) */
    private BigDecimal weight;

    /** 目标体重(kg) */
    private BigDecimal targetWeight;

    /** 年龄 */
    private Integer age;

    /** 性别: 1-男, 2-女 */
    private Integer gender;

    /** 活动量: SEDENTARY/LIGHT/MODERATE/HIGH */
    private String activityLevel;

    /** 活动系数(TDEE=BMR×系数) */
    private BigDecimal activityFactor;

    /** 健康目标: LOSE/KEEP/GAIN */
    private String healthGoal;

    /** 基础代谢率BMR(kcal/天) */
    private BigDecimal bmr;

    /** 总能量消耗TDEE(kcal/天) */
    private BigDecimal tdee;

    /** 每日推荐摄入热量(kcal/天, 按目标调整后) */
    private BigDecimal dailyCalorie;

    /** 蛋白质供能占比(%) */
    private BigDecimal proteinRatio;

    /** 碳水供能占比(%) */
    private BigDecimal carbRatio;

    /** 脂肪供能占比(%) */
    private BigDecimal fatRatio;

    /** 蛋白质目标(g/天) */
    private BigDecimal proteinGram;

    /** 碳水目标(g/天) */
    private BigDecimal carbGram;

    /** 脂肪目标(g/天) */
    private BigDecimal fatGram;

    /** 三餐比例(早/午/晚) */
    private String mealRatio;
}