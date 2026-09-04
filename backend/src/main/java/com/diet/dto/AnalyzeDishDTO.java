package com.diet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 单菜品分析请求参数(用户直接输入想吃的菜品)
 *
 * @author diet
 */
@Data
public class AnalyzeDishDTO {

    /** 菜品名称, 如: 宫保鸡丁 */
    @NotBlank(message = "请输入菜品名称")
    private String dishName;
}