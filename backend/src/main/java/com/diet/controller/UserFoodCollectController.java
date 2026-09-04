package com.diet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.entity.FoodNutrition;
import com.diet.entity.UserFoodCollect;
import com.diet.service.FoodNutritionService;
import com.diet.service.UserFoodCollectService;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户收藏食材控制器
 * 收藏数据归属当前登录用户，所有操作均按当前用户过滤
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/collect")
public class UserFoodCollectController {

    private final UserFoodCollectService collectService;

    private final FoodNutritionService foodNutritionService;

    public UserFoodCollectController(UserFoodCollectService collectService, FoodNutritionService foodNutritionService) {
        this.collectService = collectService;
        this.foodNutritionService = foodNutritionService;
    }

    /**
     * 分页查询当前用户的收藏列表
     *
     * @param current 当前页码
     * @param size    每页条数
     * @param foodId  食材ID(可选，精确过滤)
     */
    @GetMapping("/page")
    public Result<IPage<UserFoodCollect>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Long foodId) {
        LambdaQueryWrapper<UserFoodCollect> wrapper = new LambdaQueryWrapper<UserFoodCollect>()
                .eq(UserFoodCollect::getUserId, UserContext.get().getUserId())
                .eq(foodId != null, UserFoodCollect::getFoodId, foodId)
                .orderByDesc(UserFoodCollect::getCreateTime);
        return Result.success(collectService.page(new Page<>(current, size), wrapper));
    }

    /**
     * 查询当前用户全量收藏列表(关联食材营养信息，供收藏夹面板一键添加)
     * 返回字段: id(收藏ID)、foodId、fixedWeight、foodName、category、calorie 等
     */
    @GetMapping("/list")
    public Result<List<CollectItemVO>> list() {
        Long userId = UserContext.get().getUserId();
        List<UserFoodCollect> collects = collectService.lambdaQuery()
                .eq(UserFoodCollect::getUserId, userId)
                .orderByDesc(UserFoodCollect::getCreateTime)
                .list();
        if (collects.isEmpty()) {
            return Result.success(List.of());
        }
        // 批量查询关联食材，组装收藏项视图
        List<Long> foodIds = collects.stream().map(UserFoodCollect::getFoodId).toList();
        Map<Long, FoodNutrition> foodMap = foodNutritionService.listByIds(foodIds).stream()
                .collect(Collectors.toMap(FoodNutrition::getId, Function.identity()));
        List<CollectItemVO> voList = collects.stream().map(c -> {
            CollectItemVO vo = new CollectItemVO();
            vo.setId(c.getId());
            vo.setFoodId(c.getFoodId());
            vo.setFixedWeight(c.getFixedWeight());
            FoodNutrition food = foodMap.get(c.getFoodId());
            if (food != null) {
                vo.setFoodName(food.getFoodName());
                vo.setCategory(food.getCategory());
                vo.setCalorie(food.getCalorie());
                vo.setProtein(food.getProtein());
                vo.setCarbohydrate(food.getCarbohydrate());
                vo.setFat(food.getFat());
            }
            return vo;
        }).toList();
        return Result.success(voList);
    }

    /**
     * 新增收藏(同一食材重复收藏幂等处理)
     */
    @PostMapping
    public Result<Void> save(@RequestBody UserFoodCollect collect) {
        Long userId = UserContext.get().getUserId();
        collect.setUserId(userId);
        // 已存在同食材收藏则直接返回，不重复插入
        boolean exists = collectService.lambdaQuery()
                .eq(UserFoodCollect::getUserId, userId)
                .eq(UserFoodCollect::getFoodId, collect.getFoodId())
                .count() > 0;
        if (!exists) {
            if (collect.getFixedWeight() == null) {
                // 默认习惯用量100g
                collect.setFixedWeight(new BigDecimal("100"));
            }
            collectService.save(collect);
        }
        return Result.success();
    }

    /**
     * 修改收藏食材的固定重量(仅允许修改本人的收藏)
     */
    @PutMapping
    public Result<Void> update(@RequestBody UserFoodCollect collect) {
        if (collect.getId() == null) {
            return Result.fail(400, "收藏ID不能为空");
        }
        boolean updated = collectService.lambdaUpdate()
                .eq(UserFoodCollect::getId, collect.getId())
                .eq(UserFoodCollect::getUserId, UserContext.get().getUserId())
                .set(collect.getFixedWeight() != null, UserFoodCollect::getFixedWeight, collect.getFixedWeight())
                .update();
        return updated ? Result.success() : Result.fail(400, "收藏不存在或无权修改");
    }

    /**
     * 删除收藏(仅允许删除本人的收藏)
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        collectService.lambdaUpdate()
                .eq(UserFoodCollect::getId, id)
                .eq(UserFoodCollect::getUserId, UserContext.get().getUserId())
                .remove();
        return Result.success();
    }

    /**
     * 收藏项视图对象(关联食材营养基础信息)
     */
    public static class CollectItemVO {

        /** 收藏ID */
        private Long id;

        /** 食材ID */
        private Long foodId;

        /** 食材名称 */
        private String foodName;

        /** 分类 */
        private String category;

        /** 固定重量(g, 用户习惯用量) */
        private BigDecimal fixedWeight;

        /** 每100g热量(kcal) */
        private BigDecimal calorie;

        /** 每100g蛋白质(g) */
        private BigDecimal protein;

        /** 每100g碳水化合物(g) */
        private BigDecimal carbohydrate;

        /** 每100g脂肪(g) */
        private BigDecimal fat;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getFoodId() {
            return foodId;
        }

        public void setFoodId(Long foodId) {
            this.foodId = foodId;
        }

        public String getFoodName() {
            return foodName;
        }

        public void setFoodName(String foodName) {
            this.foodName = foodName;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public BigDecimal getFixedWeight() {
            return fixedWeight;
        }

        public void setFixedWeight(BigDecimal fixedWeight) {
            this.fixedWeight = fixedWeight;
        }

        public BigDecimal getCalorie() {
            return calorie;
        }

        public void setCalorie(BigDecimal calorie) {
            this.calorie = calorie;
        }

        public BigDecimal getProtein() {
            return protein;
        }

        public void setProtein(BigDecimal protein) {
            this.protein = protein;
        }

        public BigDecimal getCarbohydrate() {
            return carbohydrate;
        }

        public void setCarbohydrate(BigDecimal carbohydrate) {
            this.carbohydrate = carbohydrate;
        }

        public BigDecimal getFat() {
            return fat;
        }

        public void setFat(BigDecimal fat) {
            this.fat = fat;
        }
    }
}