package com.diet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.diet.dto.FoodCalculateDTO;
import com.diet.dto.FoodCalculateVO;
import com.diet.entity.FoodNutrition;

import java.util.List;

/**
 * 食材营养库服务接口
 *
 * @author diet
 */
public interface FoodNutritionService extends IService<FoodNutrition> {

    /**
     * 批量计算营养摄入量
     * 基于本地 food_nutrition 表数据，按 重量/100 * 每100g营养值 换算
     *
     * @param dto 计算请求(食材ID+重量列表)
     * @return 每项明细 + 三大营养素汇总
     */
    FoodCalculateVO calculate(FoodCalculateDTO dto);

    /**
     * 输入联想: 按关键词模糊匹配食材名称/别名，返回前N条
     *
     * @param keyword 关键词
     * @param limit   返回条数(默认5)
     * @return 匹配的简要食材信息列表
     */
    List<FoodNutrition> suggest(String keyword, Integer limit);
}