package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 阶段性(周/月)健康风险分析报告
 *
 * @author diet
 */
@Data
public class RiskReportVO {

    /** 分析周期描述(如: 2026-08-28 ~ 2026-09-03) */
    private String period;

    /** 有饮食记录的天数 */
    private Integer recordDays;

    /** 日均实际热量(kcal) */
    private BigDecimal avgCalorie;

    /** 日均推荐热量(kcal) */
    private BigDecimal avgRecommendCalorie;

    /** 日均蛋白质(g) */
    private BigDecimal avgProtein;

    /** 日均碳水(g) */
    private BigDecimal avgCarbohydrate;

    /** 日均脂肪(g) */
    private BigDecimal avgFat;

    /** 日均钠(mg) */
    private BigDecimal avgSodium;

    /** 日均膳食纤维(g) */
    private BigDecimal avgFiber;

    /** 碳水供能占比(%) */
    private BigDecimal carbEnergyRatio;

    /** 脂肪供能占比(%) */
    private BigDecimal fatEnergyRatio;

    /** 风险项列表(按等级降序) */
    private List<RiskItem> risks;

    /** 总体评语 */
    private String overallComment;

    /**
     * 单项风险
     */
    @Data
    public static class RiskItem {

        /** 风险类型: CALORIE/PROTEIN/SODIUM/SUGAR/FIBER/BALANCE */
        private String type;

        /** 风险名称 */
        private String name;

        /** 等级: 1-提示, 2-中风险, 3-高风险 */
        private Integer level;

        /** 等级文本 */
        private String levelText;

        /** 问题描述 */
        private String description;

        /** 改善建议 */
        private String suggestion;
    }
}