package com.diet.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 食材匹配生成食谱请求参数
 *
 * @author diet
 */
@Data
public class GenerateRecipeDTO {

    /** 用户输入的食材列表 */
    @NotEmpty(message = "请至少输入一种食材")
    private List<String> ingredients;

    /** 忌口/过敏源 */
    private String allergy;

    /** 口味偏好(如: 清淡/香辣/酸甜) */
    private String taste;

    /** 菜系(如: 家常菜/川菜/粤菜) */
    private String cuisine;

    /** 健康目标: LOSE-减脂, KEEP-维持, GAIN-增重 */
    private String healthGoal;
}