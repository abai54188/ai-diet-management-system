package com.diet.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 购物清单汇总请求参数
 *
 * @author diet
 */
@Data
public class ShoppingListDTO {

    /** 选中的菜品列表 */
    @NotEmpty(message = "请至少选择一个菜品")
    private List<AiDishInput> dishes;
}