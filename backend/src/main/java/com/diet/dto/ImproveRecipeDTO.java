package com.diet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 食谱改良请求参数
 *
 * @author diet
 */
@Data
public class ImproveRecipeDTO {

    /** 原菜品名称 */
    @NotBlank(message = "菜品名称不能为空")
    private String dishName;

    /** 原食材列表 */
    @NotEmpty(message = "原食材列表不能为空")
    private List<AiDishInput.Ingredient> ingredients;

    /** 原烹饪步骤(可选，用于改良时保持做法连续性) */
    private List<String> steps;

    /** 改良方向: LOW_CAL-低卡, LOW_FAT-低脂, HIGH_PROTEIN-高蛋白, LOW_SUGAR-控糖, LOW_SALT-低盐 */
    @NotBlank(message = "请选择改良方向")
    private String direction;
}