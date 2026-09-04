package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品入参对象(用于购物清单汇总、食谱改良等前端回传场景)
 *
 * @author diet
 */
@Data
public class AiDishInput {

    /** 菜品名称 */
    private String dishName;

    /** 食材列表 */
    private List<Ingredient> ingredients;

    /**
     * 食材入参项
     */
    @Data
    public static class Ingredient {

        /** 食材名称 */
        private String foodName;

        /** 重量(g) */
        private BigDecimal weight;
    }
}