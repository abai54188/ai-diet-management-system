package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 食谱收藏实体类(对应 recipe_collect 表)
 *
 * @author diet
 */
@Data
@TableName("recipe_collect")
public class RecipeCollect {

    /** 收藏ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 食谱ID */
    private Long recipeId;

    /** 收藏分类(自定义文件夹) */
    private String category;

    /** 创建时间 */
    private LocalDateTime createTime;
}