package com.diet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 动态发布参数
 *
 * @author diet
 */
@Data
public class PostPublishDTO {

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过100字")
    private String title;

    /** 正文内容 */
    @Size(max = 5000, message = "内容不能超过5000字")
    private String content;

    /** 图片URL列表(JSON数组字符串, 由前端拼装) */
    private List<String> images;

    /** 关联食谱ID(可选, 分享食谱场景) */
    private Long recipeId;
}