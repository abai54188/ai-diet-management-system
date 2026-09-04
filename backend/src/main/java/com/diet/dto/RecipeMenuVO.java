package com.diet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * AI生成食谱菜单视图对象
 * 一套菜单(套餐)包含多道菜品，每道菜品营养由后端基于本地营养库计算
 *
 * @author diet
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecipeMenuVO {

    /** 菜单(套餐)名称 */
    private String menuName;

    /** 套餐一句话简介 */
    private String description;

    /** 套餐包含的菜品列表 */
    private List<AiDishVO> dishes;
}