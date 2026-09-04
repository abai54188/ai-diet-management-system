package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态点赞实体类(对应 post_like 表)
 *
 * @author diet
 */
@Data
@TableName("post_like")
public class PostLike {

    /** 点赞ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 动态ID */
    private Long postId;

    /** 点赞用户ID */
    private Long userId;

    /** 创建时间 */
    private LocalDateTime createTime;
}