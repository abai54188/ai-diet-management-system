package com.diet.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论视图对象
 *
 * @author diet
 */
@Data
public class CommentVO {

    /** 评论ID */
    private Long id;

    /** 动态ID */
    private Long postId;

    /** 评论者 */
    private Long userId;
    private String nickname;

    /** 内容 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createTime;
}