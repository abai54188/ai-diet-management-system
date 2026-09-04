package com.diet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 自定义三餐比例参数(早/午/晚, 之和须为100)
 *
 * @author diet
 */
@Data
public class MealRatioDTO {

    /** 早餐占比(%) */
    @NotNull(message = "早餐占比不能为空")
    @DecimalMin(value = "10", message = "早餐占比须在10%-60%之间")
    @DecimalMax(value = "60", message = "早餐占比须在10%-60%之间")
    private BigDecimal breakfast;

    /** 午餐占比(%) */
    @NotNull(message = "午餐占比不能为空")
    @DecimalMin(value = "10", message = "午餐占比须在10%-60%之间")
    @DecimalMax(value = "60", message = "午餐占比须在10%-60%之间")
    private BigDecimal lunch;

    /** 晚餐占比(%) */
    @NotNull(message = "晚餐占比不能为空")
    @DecimalMin(value = "10", message = "晚餐占比须在10%-60%之间")
    @DecimalMax(value = "60", message = "晚餐占比须在10%-60%之间")
    private BigDecimal dinner;
}