package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 饮食记录实体类(对应 diet_record 表)
 *
 * @author diet
 */
@Data
@TableName("diet_record")
public class DietRecord {

    /** 记录ID(主键自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 食材ID */
    private Long foodId;

    /** 食材名称(冗余存储，防止食材库变更影响历史记录) */
    private String foodName;

    /** 食用重量(g) */
    private BigDecimal weight;

    /** 摄入热量(kcal, 按 weight 计算后落库) */
    private BigDecimal calorie;

    /** 蛋白质(g, AI整菜打卡时直接落库) */
    private BigDecimal protein;

    /** 碳水(g, AI整菜打卡时直接落库) */
    private BigDecimal carbohydrate;

    /** 脂肪(g, AI整菜打卡时直接落库) */
    private BigDecimal fat;

    /** 用餐时段: BREAKFAST-早餐, LUNCH-午餐, DINNER-晚餐, SNACK-加餐 */
    private String mealType;

    /** 记录日期 */
    private LocalDate recordDate;

    /** 创建时间 */
    private LocalDateTime createTime;
}