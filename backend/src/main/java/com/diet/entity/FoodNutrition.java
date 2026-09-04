package com.diet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 食材营养库实体类(对应 food_nutrition 表)
 * 数据标准: 《中国食物成分表(第6版)》，所有数值均以每100g可食部计
 *
 * @author diet
 */
@Data
@TableName("food_nutrition")
public class FoodNutrition {

    /** 食材ID(主键自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 食材名称(国内日常通用叫法) */
    private String foodName;

    /** 别名(南北通用叫法，分号分隔) */
    private String alias;

    /** 分类: 谷薯类/蔬菜类/水果类/肉禽蛋类/水产类/奶豆类/坚果类/调料类/饮品类/加工食品类 */
    private String category;

    /** 热量(kcal/100g可食部) */
    private BigDecimal calorie;

    /** 蛋白质(g/100g) */
    private BigDecimal protein;

    /** 碳水化合物(g/100g) */
    private BigDecimal carbohydrate;

    /** 脂肪(g/100g) */
    private BigDecimal fat;

    /** 膳食纤维(g/100g) */
    private BigDecimal dietaryFiber;

    /** 维生素C(mg/100g) */
    private BigDecimal vitaminC;

    /** 维生素E(mg/100g) */
    private BigDecimal vitaminE;

    /** 硫胺素VB1(mg/100g) */
    private BigDecimal vitaminB1;

    /** 核黄素VB2(mg/100g) */
    private BigDecimal vitaminB2;

    /** 钙(mg/100g) */
    private BigDecimal calcium;

    /** 铁(mg/100g) */
    private BigDecimal iron;

    /** 钠(mg/100g) */
    private BigDecimal sodium;

    /** 钾(mg/100g) */
    private BigDecimal potassium;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间(插入时自动填充) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间(插入/更新时自动填充) */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}