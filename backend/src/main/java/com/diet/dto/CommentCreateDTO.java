package com.diet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评论发布参数
 *
 * @author diet
 */
@Data
public class CommentCreateDTO {

    /** 动态ID */
    @NotNull(message = "动态ID不能为空")
    private Long postId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论不能超过500字")
    private String content;
}