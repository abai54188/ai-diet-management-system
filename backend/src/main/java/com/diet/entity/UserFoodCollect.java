package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户收藏食材实体类(对应 user_food_collect 表)
 *
 * @author diet
 */
@Data
@TableName("user_food_collect")
public class UserFoodCollect {

    /** 收藏ID(主键自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 食材ID */
    private Long foodId;

    /** 固定重量(g, 用户习惯用量) */
    private BigDecimal fixedWeight;

    /** 创建时间 */
    private LocalDateTime createTime;
}