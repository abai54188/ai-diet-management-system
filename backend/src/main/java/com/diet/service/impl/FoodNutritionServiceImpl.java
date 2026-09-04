package com.diet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.common.BusinessException;
import com.diet.dto.FoodCalculateDTO;
import com.diet.dto.FoodCalculateVO;
import com.diet.entity.FoodNutrition;
import com.diet.mapper.FoodNutritionMapper;
import com.diet.service.FoodNutritionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 食材营养库服务实现类
 *
 * @author diet
 */
@Service
public class FoodNutritionServiceImpl extends ServiceImpl<FoodNutritionMapper, FoodNutrition> implements FoodNutritionService {

    /**
     * 批量计算营养摄入量
     * 计算规则: 营养量 = 食材每100g营养值 * 重量(g) / 100
     */
    @Override
    public FoodCalculateVO calculate(FoodCalculateDTO dto) {
        FoodCalculateVO vo = new FoodCalculateVO();
        // 汇总累加器
        BigDecimal totalCalorie = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalCarb = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;

        List<FoodCalculateVO.ItemVO> itemVos = dto.getItems().stream().map(item -> {
            // 查询食材库数据
            FoodNutrition food = this.getById(item.getFoodId());
            if (food == null) {
                throw new BusinessException("食材不存在(ID:" + item.getFoodId() + ")");
            }
            // 重量换算系数: 重量/100
            BigDecimal factor = item.getWeight().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);

            FoodCalculateVO.ItemVO itemVo = new FoodCalculateVO.ItemVO();
            itemVo.setFoodId(food.getId());
            itemVo.setFoodName(food.getFoodName());
            itemVo.setCategory(food.getCategory());
            itemVo.setWeight(item.getWeight());
            itemVo.setCalorie(multiply(food.getCalorie(), factor));
            itemVo.setProtein(multiply(food.getProtein(), factor));
            itemVo.setCarbohydrate(multiply(food.getCarbohydrate(), factor));
            itemVo.setFat(multiply(food.getFat(), factor));
            return itemVo;
        }).toList();

        // 累加汇总(保留1位小数)
        for (FoodCalculateVO.ItemVO itemVo : itemVos) {
            totalCalorie = totalCalorie.add(itemVo.getCalorie());
            totalProtein = totalProtein.add(itemVo.getProtein());
            totalCarb = totalCarb.add(itemVo.getCarbohydrate());
            totalFat = totalFat.add(itemVo.getFat());
        }
        vo.setTotalCalorie(totalCalorie.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalProtein(totalProtein.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalCarbohydrate(totalCarb.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalFat(totalFat.setScale(1, RoundingMode.HALF_UP));
        vo.setItems(itemVos);
        return vo;
    }

    /**
     * 输入联想: 名称或别名模糊匹配，热量升序取前N条
     */
    @Override
    public List<FoodNutrition> suggest(String keyword, Integer limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        LambdaQueryWrapper<FoodNutrition> wrapper = new LambdaQueryWrapper<FoodNutrition>()
                .and(w -> w.like(FoodNutrition::getFoodName, keyword)
                        .or()
                        .like(FoodNutrition::getAlias, keyword))
                .orderByAsc(FoodNutrition::getCalorie)
                .last("LIMIT " + (limit == null || limit < 1 ? 5 : Math.min(limit, 20)));
        return this.list(wrapper);
    }

    /**
     * 营养值 * 系数，结果保留1位小数(四舍五入)
     */
    private BigDecimal multiply(BigDecimal base, BigDecimal factor) {
        return base.multiply(factor).setScale(1, RoundingMode.HALF_UP);
    }
}