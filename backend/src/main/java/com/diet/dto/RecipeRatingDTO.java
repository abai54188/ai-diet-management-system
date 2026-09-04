package com.diet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 食谱评分参数
 *
 * @author diet
 */
@Data
public class RecipeRatingDTO {

    /** 食谱ID */
    @NotNull(message = "食谱ID不能为空")
    private Long recipeId;

    /** 评分1-5星 */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1星")
    @Max(value = 5, message = "评分最高5星")
    private Integer score;

    /** 文字反馈 */
    private String feedback;
}