package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物清单汇总结果(按 蔬菜/肉类水产/调料/其他 分组)
 *
 * @author diet
 */
@Data
public class ShoppingListVO {

    /** 分类分组 */
    private List<CategoryGroup> categories;

    /**
     * 清单分组
     */
    @Data
    public static class CategoryGroup {

        /** 分组名称: 蔬菜/肉类水产/调料/其他 */
        private String category;

        /** 组内食材 */
        private List<ShoppingItem> items;
    }

    /**
     * 清单食材项(跨菜品重量自动累加)
     */
    @Data
    public static class ShoppingItem {

        /** 食材名称 */
        private String foodName;

        /** 合计重量(g) */
        private BigDecimal weight;

        /** 是否匹配本地营养库 */
        private Boolean matched;

        /** 营养库原始分类(如: 蔬菜类) */
        private String sourceCategory;
    }
}