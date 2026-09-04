package com.diet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 热量计算请求参数(支持批量传入多个食材)
 *
 * @author diet
 */
@Data
public class FoodCalculateDTO {

    /** 食材明细列表 */
    @NotEmpty(message = "食材列表不能为空")
    @Valid
    private List<Item> items;

    /**
     * 单个食材计算项
     */
    @Data
    public static class Item {

        /** 食材ID */
        @NotNull(message = "食材ID不能为空")
        @Min(value = 1, message = "食材ID不合法")
        private Long foodId;

        /** 食用重量(g) */
        @NotNull(message = "重量不能为空")
        @DecimalMin(value = "0.1", message = "重量须大于0")
        @Max(value = 10000, message = "单次录入重量不能超过10kg")
        private BigDecimal weight;
    }
}