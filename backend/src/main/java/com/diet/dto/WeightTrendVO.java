package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 体重趋势与达标预测视图对象
 * 热量缺口原理: 7700kcal ≈ 1kg脂肪
 *
 * @author diet
 */
@Data
public class WeightTrendVO {

    /** 体重记录列表(按日期升序) */
    private List<WeightPoint> records;

    /** 当前体重(kg) */
    private BigDecimal currentWeight;

    /** 目标体重(kg) */
    private BigDecimal targetWeight;

    /** 距目标还需变化量(kg, 正为需增重) */
    private BigDecimal remainingKg;

    /** 日均体重变化(kg/天, 负为下降) */
    private BigDecimal dailyChange;

    /** 热量缺口(kcal/天): 推荐TDEE-实际日均摄入, 与体重变化交叉验证 */
    private BigDecimal calorieGap;

    /** 预计达标天数(null表示无法预测) */
    private Integer forecastDays;

    /** 预计达标日期 */
    private String forecastDate;

    /** 趋势说明与建议 */
    private String suggestion;

    /**
     * 体重记录点
     */
    @Data
    public static class WeightPoint {

        /** 记录日期 */
        private String date;

        /** 体重(kg) */
        private BigDecimal weight;
    }
}