package com.diet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 一周食谱规划请求参数
 *
 * @author diet
 */
@Data
public class WeekPlanDTO {

    /** 健康目标: LOSE-减脂, KEEP-维持, GAIN-增重 */
    private String healthGoal;

    /** 用餐人数(食材重量按人数放大) */
    @NotNull(message = "请填写用餐人数")
    @Min(value = 1, message = "用餐人数至少为1人")
    @Max(value = 20, message = "用餐人数不能超过20人")
    private Integer servings;

    /** 忌口/过敏源 */
    private String allergy;
}