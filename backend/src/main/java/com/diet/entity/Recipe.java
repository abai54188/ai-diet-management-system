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
 * 食谱实体类(对应 recipe 表)
 *
 * @author diet
 */
@Data
@TableName("recipe")
public class Recipe {

    /** 食谱ID(主键自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 食谱名称 */
    private String recipeName;

    /** 做法(步骤说明) */
    private String cookingMethod;

    /** 食材列表(JSON数组: [{foodId,foodName,weight}]) */
    private String ingredients;

    /** 单份热量(kcal) */
    private BigDecimal caloriePerServing;

    /** 难度: 1-简单, 2-一般, 3-较难 */
    private Integer difficulty;

    /** 耗时(分钟) */
    private Integer cookingTime;

    /** 创建人用户ID(官方食谱为NULL) */
    private Long userId;

    /** 是否官方: 0-用户创建, 1-官方食谱 */
    private Integer isOfficial;

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