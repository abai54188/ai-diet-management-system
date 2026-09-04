package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区动态评论实体类(对应 post_comment 表)
 *
 * @author diet
 */
@Data
@TableName("post_comment")
public class PostComment {

    /** 评论ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 动态ID */
    private Long postId;

    /** 评论用户ID */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 审核状态: 0-正常, 1-屏蔽 */
    private Integer status;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;
}