package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户健康档案实体类(对应 health_profile 表)
 *
 * @author diet
 */
@Data
@TableName("health_profile")
public class HealthProfile {

    /** 档案ID(主键自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID(唯一) */
    private Long userId;

    /** 每日推荐热量(kcal) */
    private BigDecimal dailyCalorie;

    /** 蛋白质供能占比(%) */
    private BigDecimal proteinRatio;

    /** 碳水供能占比(%) */
    private BigDecimal carbRatio;

    /** 脂肪供能占比(%) */
    private BigDecimal fatRatio;

    /** 三餐比例(早,午,晚供能占比%) */
    private String mealRatio;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}