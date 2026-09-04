package com.diet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI食物营养解析请求参数
 * 用户输入食物或菜品名称(如"番茄炒蛋")，由AI解析食材组成后本地计算营养
 *
 * @author diet
 */
@Data
public class AiFoodParseDTO {

    /** 食物/菜品名称 */
    @NotBlank(message = "食物名称不能为空")
    @Size(max = 50, message = "名称不能超过50字")
    private String foodName;

    /** 份数(默认1份，AI按此倍数放大食材重量) */
    private BigDecimal servings;

    /** 食物总重量(g, 可含小数)。提供后按比例将全部食材缩放到该总重量，覆盖servings */
    private BigDecimal totalWeight;
}