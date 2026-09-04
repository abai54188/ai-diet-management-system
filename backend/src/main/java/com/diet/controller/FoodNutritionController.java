package com.diet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.BusinessException;
import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.dto.FoodCalculateDTO;
import com.diet.dto.FoodCalculateVO;
import com.diet.entity.FoodNutrition;
import com.diet.service.FoodNutritionService;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 食材营养库控制器
 * 提供食材搜索、营养计算、输入联想、详情查询与收藏夹维护
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/food")
public class FoodNutritionController {

    private final FoodNutritionService foodNutritionService;

    public FoodNutritionController(FoodNutritionService foodNutritionService) {
        this.foodNutritionService = foodNutritionService;
    }

    /**
     * 分页搜索食材(名称/别名模糊 + 分类筛选 + 热量区间)
     *
     * @param current    当前页码，默认1
     * @param size       每页条数，默认10
     * @param keyword    食材名称/别名关键字(模糊匹配)
     * @param category   分类精确匹配
     * @param minCalorie 热量下限(kcal)
     * @param maxCalorie 热量上限(kcal)
     */
    @GetMapping("/page")
    public Result<IPage<FoodNutrition>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) BigDecimal minCalorie,
                                             @RequestParam(required = false) BigDecimal maxCalorie) {
        LambdaQueryWrapper<FoodNutrition> wrapper = new LambdaQueryWrapper<FoodNutrition>()
                // 名称或别名任一命中即返回
                .and(StringUtils.hasText(keyword), w -> w
                        .like(FoodNutrition::getFoodName, keyword)
                        .or()
                        .like(FoodNutrition::getAlias, keyword))
                .eq(StringUtils.hasText(category), FoodNutrition::getCategory, category)
                .ge(minCalorie != null, FoodNutrition::getCalorie, minCalorie)
                .le(maxCalorie != null, FoodNutrition::getCalorie, maxCalorie)
                .orderByAsc(FoodNutrition::getCalorie);
        return Result.success(foodNutritionService.page(new Page<>(current, size), wrapper));
    }

    /**
     * 输入联想接口: 返回前5条匹配食材(名称/别名模糊)
     *
     * @param keyword 用户输入的关键词
     * @param limit   返回条数，默认5，最大20
     */
    @GetMapping("/suggest")
    public Result<List<FoodNutrition>> suggest(@RequestParam String keyword,
                                               @RequestParam(required = false, defaultValue = "5") Integer limit) {
        return Result.success(foodNutritionService.suggest(keyword, limit));
    }

    /**
     * 批量热量计算接口: 传入食材ID+重量列表，返回每项营养量与三大营养素汇总
     * 全部数据基于本地 food_nutrition 表换算，非AI估算
     */
    @PostMapping("/calculate")
    public Result<FoodCalculateVO> calculate(@Validated @RequestBody FoodCalculateDTO dto) {
        return Result.success(foodNutritionService.calculate(dto));
    }

    /**
     * 食材详情接口: 返回完整营养成分表(膳食纤维/维生素/矿物质等全部字段)
     */
    @GetMapping("/{id}")
    public Result<FoodNutrition> detail(@PathVariable Long id) {
        FoodNutrition food = foodNutritionService.getById(id);
        if (food == null) {
            throw new BusinessException("食材不存在");
        }
        return Result.success(food);
    }

    /**
     * 按分类查询所有食材(不分页，用于分类浏览)
     */
    @GetMapping("/list")
    public Result<List<FoodNutrition>> listByCategory(@RequestParam(required = false) String category) {
        LambdaQueryWrapper<FoodNutrition> wrapper = new LambdaQueryWrapper<FoodNutrition>()
                .eq(StringUtils.hasText(category), FoodNutrition::getCategory, category)
                .orderByAsc(FoodNutrition::getCalorie);
        return Result.success(foodNutritionService.list(wrapper));
    }

    /**
     * 新增食材(管理员维护营养库)
     */
    @PostMapping
    public Result<Void> save(@RequestBody FoodNutrition food) {
        foodNutritionService.save(food);
        return Result.success();
    }

    /**
     * 修改食材信息
     */
    @PutMapping
    public Result<Void> update(@RequestBody FoodNutrition food) {
        if (food.getId() == null) {
            throw new BusinessException("食材ID不能为空");
        }
        foodNutritionService.updateById(food);
        return Result.success();
    }

    /**
     * 删除食材
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        foodNutritionService.removeById(id);
        return Result.success();
    }
}