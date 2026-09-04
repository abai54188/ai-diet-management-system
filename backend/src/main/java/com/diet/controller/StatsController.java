package com.diet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.common.BusinessException;
import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.entity.DietRecord;
import com.diet.entity.FoodNutrition;
import com.diet.mapper.DietRecordMapper;
import com.diet.mapper.FoodNutritionMapper;
import com.diet.service.HealthCalcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 饮食数据可视化看板控制器
 * 提供周/月热量趋势、营养素占比、食材分类、分时段、达标率五类统计
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final DietRecordMapper dietRecordMapper;
    private final FoodNutritionMapper foodMapper;
    private final HealthCalcService healthCalcService;

    public StatsController(DietRecordMapper dietRecordMapper, FoodNutritionMapper foodMapper,
                           HealthCalcService healthCalcService) {
        this.dietRecordMapper = dietRecordMapper;
        this.foodMapper = foodMapper;
        this.healthCalcService = healthCalcService;
    }

    /**
     * 周/月热量摄入趋势(按日期逐日汇总)
     *
     * @param days 周期: 7-周, 30-月
     */
    @GetMapping("/calorie-trend")
    public Result<List<Map<String, Object>>> calorieTrend(@RequestParam(defaultValue = "7") Integer days) {
        Long userId = UserContext.get().getUserId();
        int range = days == null || days < 1 ? 7 : Math.min(days, 90);
        LocalDate start = LocalDate.now().minusDays(range - 1L);
        List<DietRecord> records = dietRecordMapper.selectList(new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, userId)
                .ge(DietRecord::getRecordDate, start)
                .orderByAsc(DietRecord::getRecordDate));
        // 按日期分组求和(无记录日补0, 保证折线连续)
        Map<LocalDate, BigDecimal> byDate = records.stream()
                .collect(Collectors.groupingBy(DietRecord::getRecordDate,
                        Collectors.reducing(BigDecimal.ZERO,
                                r -> r.getCalorie() == null ? BigDecimal.ZERO : r.getCalorie(),
                                BigDecimal::add)));
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < range; i++) {
            LocalDate d = start.plusDays(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", d.toString());
            point.put("calorie", byDate.getOrDefault(d, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP));
            list.add(point);
        }
        return Result.success(list);
    }

    /**
     * 三大营养素占比统计(周期内总量的供能占比饼图)
     */
    @GetMapping("/macro-ratio")
    public Result<Map<String, Object>> macroRatio(@RequestParam(defaultValue = "7") Integer days) {
        var macroSum = sumMacro(days);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protein", macroSum.get("protein"));
        result.put("carbohydrate", macroSum.get("carbohydrate"));
        result.put("fat", macroSum.get("fat"));
        // 供能占比
        BigDecimal p = macroSum.get("protein").multiply(new BigDecimal("4"));
        BigDecimal c = macroSum.get("carbohydrate").multiply(new BigDecimal("4"));
        BigDecimal f = macroSum.get("fat").multiply(new BigDecimal("9"));
        BigDecimal total = p.add(c).add(f);
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            result.put("proteinRatio", p.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP));
            result.put("carbRatio", c.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP));
            result.put("fatRatio", f.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP));
        }
        return Result.success(result);
    }

    /**
     * 食材分类统计(周期内记录按分类计数与总重量)
     */
    @GetMapping("/food-category")
    public Result<List<Map<String, Object>>> foodCategory(@RequestParam(defaultValue = "7") Integer days) {
        Long userId = UserContext.get().getUserId();
        List<DietRecord> records = queryRecords(userId, days);
        Map<Long, FoodNutrition> foodMap = loadFoods(records);
        // 分类 -> [次数, 总重量]
        Map<String, List<BigDecimal>> byCategory = new LinkedHashMap<>();
        for (DietRecord r : records) {
            FoodNutrition food = foodMap.get(r.getFoodId());
            String category = food != null ? food.getCategory() : "未分类";
            List<BigDecimal> agg = byCategory.computeIfAbsent(category,
                    k -> new ArrayList<>(List.of(BigDecimal.ZERO, BigDecimal.ZERO)));
            agg.set(0, agg.get(0).add(BigDecimal.ONE));
            agg.set(1, agg.get(1).add(r.getWeight() == null ? BigDecimal.ZERO : r.getWeight()));
        }
        return Result.success(byCategory.entrySet().stream()
                .sorted((a, b) -> b.getValue().get(0).compareTo(a.getValue().get(0)))
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("category", e.getKey());
                    item.put("count", e.getValue().get(0).intValue());
                    item.put("weight", e.getValue().get(1).setScale(0, RoundingMode.HALF_UP));
                    return item;
                }).toList());
    }

    /**
     * 分时段摄入统计(早/午/晚/加餐的热量与次数)
     */
    @GetMapping("/meal-period")
    public Result<List<Map<String, Object>>> mealPeriod(@RequestParam(defaultValue = "7") Integer days) {
        Long userId = UserContext.get().getUserId();
        List<DietRecord> records = queryRecords(userId, days);
        String[] types = {"BREAKFAST", "LUNCH", "DINNER", "SNACK"};
        String[] labels = {"早餐", "午餐", "晚餐", "加餐"};
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            List<DietRecord> mealRecords = records.stream()
                    .filter(r -> type.equals(r.getMealType())).toList();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("mealType", type);
            item.put("label", labels[i]);
            item.put("count", mealRecords.size());
            item.put("calorie", mealRecords.stream()
                    .map(r -> r.getCalorie() == null ? BigDecimal.ZERO : r.getCalorie())
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(1, RoundingMode.HALF_UP));
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 达标率统计(周期内每日热量在推荐值±10%视为达标)
     */
    @GetMapping("/achievement")
    public Result<Map<String, Object>> achievement(@RequestParam(defaultValue = "7") Integer days) {
        Long userId = UserContext.get().getUserId();
        int range = days == null || days < 1 ? 7 : Math.min(days, 90);
        // 推荐热量(未完善档案时报错)
        BigDecimal recommend;
        try {
            recommend = healthCalcService.getProfile(userId).getDailyCalorie();
        } catch (BusinessException e) {
            throw new BusinessException("请先完善健康档案以计算达标率");
        }
        List<DietRecord> records = queryRecords(userId, range);
        // 按日汇总
        Map<LocalDate, BigDecimal> byDate = records.stream()
                .collect(Collectors.groupingBy(DietRecord::getRecordDate,
                        Collectors.reducing(BigDecimal.ZERO,
                                r -> r.getCalorie() == null ? BigDecimal.ZERO : r.getCalorie(),
                                BigDecimal::add)));
        int hitDays = 0;
        int recordDays = 0;
        for (Map.Entry<LocalDate, BigDecimal> e : byDate.entrySet()) {
            recordDays++;
            // 达标: 实际在推荐的90%-110%之间
            if (e.getValue().compareTo(recommend.multiply(new BigDecimal("0.9"))) >= 0
                    && e.getValue().compareTo(recommend.multiply(new BigDecimal("1.1"))) <= 0) {
                hitDays++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recommend", recommend);
        result.put("recordDays", recordDays);
        result.put("hitDays", hitDays);
        result.put("rate", recordDays == 0 ? 0
                : BigDecimal.valueOf(hitDays * 100.0 / recordDays).setScale(1, RoundingMode.HALF_UP));
        return Result.success(result);
    }

    /**
     * 查询周期内饮食记录
     */
    private List<DietRecord> queryRecords(Long userId, Integer days) {
        int range = days == null || days < 1 ? 7 : Math.min(days, 90);
        return dietRecordMapper.selectList(new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, userId)
                .ge(DietRecord::getRecordDate, LocalDate.now().minusDays(range - 1L)));
    }

    /**
     * 批量加载记录关联的食材
     */
    private Map<Long, FoodNutrition> loadFoods(List<DietRecord> records) {
        if (records.isEmpty()) {
            return Map.of();
        }
        return foodMapper.selectBatchIds(records.stream().map(DietRecord::getFoodId).distinct().toList())
                .stream().collect(Collectors.toMap(FoodNutrition::getId, Function.identity()));
    }

    /**
     * 周期内三大营养素总量(基于本地营养库换算)
     */
    private Map<String, BigDecimal> sumMacro(Integer days) {
        Long userId = UserContext.get().getUserId();
        List<DietRecord> records = queryRecords(userId, days);
        Map<Long, FoodNutrition> foodMap = loadFoods(records);
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carb = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        for (DietRecord r : records) {
            FoodNutrition food = foodMap.get(r.getFoodId());
            if (food == null) {
                continue;
            }
            BigDecimal f = r.getWeight().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
            if (food.getProtein() != null) {
                protein = protein.add(food.getProtein().multiply(f));
            }
            if (food.getCarbohydrate() != null) {
                carb = carb.add(food.getCarbohydrate().multiply(f));
            }
            if (food.getFat() != null) {
                fat = fat.add(food.getFat().multiply(f));
            }
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put("protein", protein.setScale(1, RoundingMode.HALF_UP));
        result.put("carbohydrate", carb.setScale(1, RoundingMode.HALF_UP));
        result.put("fat", fat.setScale(1, RoundingMode.HALF_UP));
        return result;
    }
}