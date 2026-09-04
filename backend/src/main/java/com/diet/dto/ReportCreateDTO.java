package com.diet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 内容举报参数
 *
 * @author diet
 */
@Data
public class ReportCreateDTO {

    /** 目标类型: POST-动态, COMMENT-评论 */
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    /** 目标ID */
    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    /** 举报理由 */
    @NotBlank(message = "举报理由不能为空")
    @Size(max = 200, message = "理由不能超过200字")
    private String reason;
}