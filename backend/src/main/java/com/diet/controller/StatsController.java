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
import com.diet.util.AiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private final AiClient aiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 大模型菜品分类结果缓存(菜名 -> 标准分类), 避免同周期重复调用 */
    private final Map<String, String> categoryCache = new ConcurrentHashMap<>();

    /** 大模型菜品营养估算缓存(菜名 -> [蛋白,碳水,脂肪]g/100g), 仅用于无任何营养数据的记录 */
    private final Map<String, BigDecimal[]> macroCache = new ConcurrentHashMap<>();

    /** 标准分类集合缓存(来自本地营养库去重, 末尾追加"其他") */
    private volatile List<String> standardCategories;

    public StatsController(DietRecordMapper dietRecordMapper, FoodNutritionMapper foodMapper,
                           HealthCalcService healthCalcService, AiClient aiClient) {
        this.dietRecordMapper = dietRecordMapper;
        this.foodMapper = foodMapper;
        this.healthCalcService = healthCalcService;
        this.aiClient = aiClient;
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
     * 未命中本地营养库的记录(AI整菜打卡等)由大模型识别归类, 保证不出现"未分类"
     */
    @GetMapping("/food-category")
    public Result<List<Map<String, Object>>> foodCategory(@RequestParam(defaultValue = "7") Integer days) {
        Long userId = UserContext.get().getUserId();
        List<DietRecord> records = queryRecords(userId, days);
        Map<Long, FoodNutrition> foodMap = loadFoods(records);
        // 1. 先用本地库分类, 收集未命中的菜名
        Set<String> unknownNames = new LinkedHashSet<>();
        Map<Long, String> recordCategory = new LinkedHashMap<>();
        for (DietRecord r : records) {
            FoodNutrition food = r.getFoodId() == null ? null : foodMap.get(r.getFoodId());
            if (food != null && StringUtils.hasText(food.getCategory())) {
                recordCategory.put(r.getId(), food.getCategory());
            } else if (StringUtils.hasText(r.getFoodName())) {
                unknownNames.add(r.getFoodName().trim());
            } else {
                recordCategory.put(r.getId(), "其他");
            }
        }
        // 2. 未命中菜名批量交给大模型归类到标准分类
        if (!unknownNames.isEmpty()) {
            Map<String, String> classified = classifyByAi(unknownNames);
            for (DietRecord r : records) {
                if (!recordCategory.containsKey(r.getId()) && StringUtils.hasText(r.getFoodName())) {
                    recordCategory.put(r.getId(),
                            classified.getOrDefault(r.getFoodName().trim(), "其他"));
                }
            }
        }
        // 3. 分类 -> [次数, 总重量]
        Map<String, List<BigDecimal>> byCategory = new LinkedHashMap<>();
        for (DietRecord r : records) {
            String category = recordCategory.getOrDefault(r.getId(), "其他");
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
        List<Long> foodIds = records.stream().map(DietRecord::getFoodId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (foodIds.isEmpty()) {
            return Map.of();
        }
        return foodMapper.selectBatchIds(foodIds)
                .stream().collect(Collectors.toMap(FoodNutrition::getId, Function.identity()));
    }

    /**
     * 周期内三大营养素总量
     * 1. 命中本地营养库的记录按库换算;
     * 2. 未命中的记录(AI整菜打卡)优先使用落库时的蛋白/碳水/脂肪;
     * 3. 三项全缺的记录由大模型按菜名估算每100g营养后按重量折算
     */
    private Map<String, BigDecimal> sumMacro(Integer days) {
        Long userId = UserContext.get().getUserId();
        List<DietRecord> records = queryRecords(userId, days);
        Map<Long, FoodNutrition> foodMap = loadFoods(records);
        // 收集三项营养全缺且未命中本地库的菜品名, 批量交给大模型估算
        Set<String> needEstimate = new LinkedHashSet<>();
        for (DietRecord r : records) {
            FoodNutrition food = r.getFoodId() == null ? null : foodMap.get(r.getFoodId());
            if (food == null && r.getProtein() == null && r.getCarbohydrate() == null && r.getFat() == null
                    && StringUtils.hasText(r.getFoodName())) {
                needEstimate.add(r.getFoodName().trim());
            }
        }
        if (!needEstimate.isEmpty()) {
            estimateMacrosByAi(needEstimate);
        }
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carb = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        for (DietRecord r : records) {
            BigDecimal w = r.getWeight() == null ? BigDecimal.ZERO : r.getWeight();
            FoodNutrition food = r.getFoodId() == null ? null : foodMap.get(r.getFoodId());
            if (food != null) {
                BigDecimal f = w.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                if (food.getProtein() != null) {
                    protein = protein.add(food.getProtein().multiply(f));
                }
                if (food.getCarbohydrate() != null) {
                    carb = carb.add(food.getCarbohydrate().multiply(f));
                }
                if (food.getFat() != null) {
                    fat = fat.add(food.getFat().multiply(f));
                }
                continue;
            }
            // AI整菜打卡: 优先用落库营养值
            if (r.getProtein() != null || r.getCarbohydrate() != null || r.getFat() != null) {
                protein = protein.add(r.getProtein() == null ? BigDecimal.ZERO : r.getProtein());
                carb = carb.add(r.getCarbohydrate() == null ? BigDecimal.ZERO : r.getCarbohydrate());
                fat = fat.add(r.getFat() == null ? BigDecimal.ZERO : r.getFat());
                continue;
            }
            // 三项全缺: 用大模型估算的每100g营养按重量折算
            BigDecimal[] est = macroCache.get(r.getFoodName() == null ? "" : r.getFoodName().trim());
            if (est != null) {
                BigDecimal f = w.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                protein = protein.add(est[0].multiply(f));
                carb = carb.add(est[1].multiply(f));
                fat = fat.add(est[2].multiply(f));
            }
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put("protein", protein.setScale(1, RoundingMode.HALF_UP));
        result.put("carbohydrate", carb.setScale(1, RoundingMode.HALF_UP));
        result.put("fat", fat.setScale(1, RoundingMode.HALF_UP));
        return result;
    }

    /**
     * 标准分类集合: 本地营养库去重分类 + "其他"(兜底)
     */
    private List<String> standardCategories() {
        List<String> cached = standardCategories;
        if (cached == null) {
            synchronized (this) {
                if (standardCategories == null) {
                    List<Object> objs = foodMapper.selectObjs(new LambdaQueryWrapper<FoodNutrition>()
                            .select(FoodNutrition::getCategory)
                            .groupBy(FoodNutrition::getCategory));
                    List<String> list = objs.stream()
                            .filter(o -> o != null && StringUtils.hasText(o.toString()))
                            .map(Object::toString)
                            .sorted()
                            .collect(Collectors.toCollection(ArrayList::new));
                    list.add("其他");
                    standardCategories = list;
                }
                cached = standardCategories;
            }
        }
        return cached;
    }

    /**
     * 大模型批量归类: 将菜名划分到标准分类之一
     * 未配置密钥/调用失败/解析失败时静默降级为"其他", 不产生"未分类"
     */
    private Map<String, String> classifyByAi(Set<String> names) {
        Map<String, String> result = new LinkedHashMap<>();
        List<String> pending = names.stream().filter(n -> !categoryCache.containsKey(n)).toList();
        if (!pending.isEmpty() && aiClient.isConfigured()) {
            try {
                List<String> cats = standardCategories();
                String systemPrompt = "你是中式食材分类专家。把用户给出的每个菜品/食物名称划分到给定分类表中最合适的一项，"
                        + "严格输出JSON对象(菜名为key, 分类为value)，不要输出任何其他文字。";
                String userPrompt = "分类表: " + String.join("、", cats)
                        + "\n菜品列表: " + String.join("、", pending)
                        + "\n要求: value必须是分类表中的原值，实在难以判断的填\"其他\"。";
                String content = aiClient.chat(systemPrompt, userPrompt);
                JsonNode root = objectMapper.readTree(extractJson(content));
                for (String name : pending) {
                    String cat = root.path(name).asText("");
                    categoryCache.put(name, cats.contains(cat) ? cat : "其他");
                }
            } catch (Exception e) {
                // 失败兜底: 全部记为"其他"
                pending.forEach(n -> categoryCache.put(n, "其他"));
            }
        } else if (!pending.isEmpty()) {
            pending.forEach(n -> categoryCache.putIfAbsent(n, "其他"));
        }
        names.forEach(n -> result.put(n, categoryCache.getOrDefault(n, "其他")));
        return result;
    }

    /**
     * 大模型批量估算: 为三项营养全缺的菜品估算每100g蛋白/碳水/脂肪
     * 结果写入 macroCache; 失败时跳过该菜品(保持原有不计入口径)
     */
    private void estimateMacrosByAi(Set<String> names) {
        List<String> pending = names.stream().filter(n -> !macroCache.containsKey(n)).toList();
        if (pending.isEmpty() || !aiClient.isConfigured()) {
            return;
        }
        try {
            String systemPrompt = "你是营养估算专家。为用户给出的每个菜品估算每100克可食部的蛋白质、碳水化合物、脂肪克数，"
                    + "严格输出JSON对象(菜名为key, 值为含protein/carbohydrate/fat数字的对象)，不要输出任何其他文字。";
            String userPrompt = "菜品列表: " + String.join("、", pending)
                    + "\n输出示例: {\"番茄炒蛋\": {\"protein\": 5.2, \"carbohydrate\": 4.1, \"fat\": 8.3}}";
            String content = aiClient.chat(systemPrompt, userPrompt);
            JsonNode root = objectMapper.readTree(extractJson(content));
            for (String name : pending) {
                JsonNode node = root.path(name);
                if (node.isObject()) {
                    macroCache.put(name, new BigDecimal[]{
                            numOrZero(node.path("protein")),
                            numOrZero(node.path("carbohydrate")),
                            numOrZero(node.path("fat"))});
                }
            }
        } catch (Exception e) {
            // 估算失败保持原口径(不计入), 不影响整体统计
        }
    }

    /** 从模型回复中截取首个JSON对象 */
    private String extractJson(String content) {
        String t = content == null ? "" : content.trim();
        if (t.startsWith("```")) {
            t = t.replaceAll("^```(json)?", "").replaceAll("```+$", "").trim();
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException("AI返回数据解析失败");
        }
        return t.substring(start, end + 1);
    }

    /** 数字节点读取, 缺失/非法按0 */
    private BigDecimal numOrZero(JsonNode n) {
        return (n != null && n.isNumber()) ? n.decimalValue() : BigDecimal.ZERO;
    }
}