package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户健康信息更新参数(身高/体重/年龄/性别/活动量/健康目标/忌口)
 *
 * @author diet
 */
@Data
public class UserProfileDTO {

    /** 身高(cm) */
    private BigDecimal height;

    /** 体重(kg) */
    private BigDecimal weight;

    /** 目标体重(kg) */
    private BigDecimal targetWeight;

    /** 年龄(岁) */
    private Integer age;

    /** 性别: 0-未知, 1-男, 2-女 */
    private Integer gender;

    /** 活动量: SEDENTARY-久坐, LIGHT-轻度, MODERATE-中度, HIGH-高度 */
    private String activityLevel;

    /** 健康目标: LOSE-减脂, KEEP-维持, GAIN-增重 */
    private String healthGoal;

    /** 忌口/过敏源(逗号分隔) */
    private String allergy;
}