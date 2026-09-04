package com.diet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 替换单餐菜品请求参数
 *
 * @author diet
 */
@Data
public class ReplaceDishDTO {

    /** 用餐时段: BREAKFAST-早餐, LUNCH-午餐, DINNER-晚餐 */
    @NotBlank(message = "用餐时段不能为空")
    private String mealType;

    /** 星期标签(如: 周一) */
    private String dayLabel;

    /** 健康目标: LOSE/KEEP/GAIN */
    private String healthGoal;

    /** 忌口/过敏源 */
    private String allergy;

    /** 口味偏好 */
    private String taste;

    /** 菜系 */
    private String cuisine;

    /** 需避开的菜品名称(当前日三餐，避免替换后重复) */
    private List<String> excludeDishNames;

    /** 用餐人数(新菜食材重量按此放大) */
    private Integer servings;
}