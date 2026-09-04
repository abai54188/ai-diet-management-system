package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 食谱评分反馈实体类(对应 recipe_rating 表)
 *
 * @author diet
 */
@Data
@TableName("recipe_rating")
public class RecipeRating {

    /** 评分ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 食谱ID */
    private Long recipeId;

    /** 评分用户ID */
    private Long userId;

    /** 评分1-5星 */
    private Integer score;

    /** 文字反馈 */
    private String feedback;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;
}