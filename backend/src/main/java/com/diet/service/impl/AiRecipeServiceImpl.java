package com.diet.service.impl;

import com.diet.common.BusinessException;
import com.diet.dto.AiDishInput;
import com.diet.dto.AiDishVO;
import com.diet.dto.AiIngredientVO;
import com.diet.dto.GenerateRecipeDTO;
import com.diet.dto.ImproveRecipeDTO;
import com.diet.dto.ImproveRecipeVO;
import com.diet.dto.ReplaceDishDTO;
import com.diet.dto.ShoppingListDTO;
import com.diet.dto.ShoppingListVO;
import com.diet.dto.WeekPlanDTO;
import com.diet.dto.WeekPlanVO;
import com.diet.entity.FoodNutrition;
import com.diet.service.AiRecipeService;
import com.diet.service.NutritionCalculator;
import com.diet.util.AiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI智能食谱服务实现类
 *
 * 实现要点:
 * 1. 真实模式: 加载prompts目录模板 -> AiClient调用大模型 -> 解析JSON -> 本地营养库计算
 * 2. 模拟模式(ai.mock=true): 未配置Key时使用本地菜品模板联调，营养计算链路与真实模式完全一致
 * 3. 硬性约束: AI仅输出食材名称+重量，热量/三大营养素一律由NutritionCalculator本地计算
 *
 * @author diet
 */
@Service
public class AiRecipeServiceImpl implements AiRecipeService {

    /** 系统提示词: 约束AI角色与输出格式 */
    private static final String SYSTEM_PROMPT =
            "你是一名专业中式营养配餐师。严格按用户要求的JSON格式输出，禁止输出任何热量数值，禁止输出JSON以外的任何文字。";

    /** 星期标签 */
    private static final String[] DAY_LABELS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    /** 购物清单分组顺序 */
    private static final List<String> SHOPPING_ORDER = List.of("蔬菜", "肉类水产", "调料", "其他");

    private static final Random RANDOM = new Random();

    /** Prompt模板缓存(避免重复读文件) */
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    private final AiClient aiClient;

    private final NutritionCalculator calculator;

    private final ObjectMapper objectMapper;

    /** 是否启用本地模拟模式(未配置真实Key时联调用) */
    @Value("${ai.mock:true}")
    private boolean mock;

    public AiRecipeServiceImpl(AiClient aiClient, NutritionCalculator calculator, ObjectMapper objectMapper,
                               com.diet.service.UserService userService) {
        this.aiClient = aiClient;
        this.calculator = calculator;
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    /** 用户服务(读取忌口/过敏源配置) */
    private final com.diet.service.UserService userService;

    // ==================== 1. 食材匹配生成食谱 ====================

    @Override
    public List<AiDishVO> generateRecipes(GenerateRecipeDTO dto) {
        List<AiDishVO> dishes;
        if (mock) {
            dishes = mockGenerateDishes(dto);
        } else {
            // 生成3-5套菜品
            int count = 3 + RANDOM.nextInt(3);
            String userPrompt = loadPrompt("generate_recipe_prompt.txt")
                    .replace("{{count}}", String.valueOf(count))
                    .replace("{{ingredients}}", String.join("、", dto.getIngredients()))
                    .replace("{{allergy}}", orDefault(dto.getAllergy()))
                    .replace("{{taste}}", orDefault(dto.getTaste()))
                    .replace("{{cuisine}}", orDefault(dto.getCuisine()))
                    .replace("{{healthGoal}}", goalText(dto.getHealthGoal()));
            dishes = parseDishes(aiClient.chat(SYSTEM_PROMPT, userPrompt));
            if (dishes.size() > count) {
                dishes = new ArrayList<>(dishes.subList(0, count));
            }
        }
        // 忌口自动过滤(用户档案设置的过敏源)
        applyAllergyFilter(dishes, effectiveAllergy(dto.getAllergy()));
        // 营养全部本地计算(硬性约束)
        dishes.forEach(calculator::fillDishNutrition);
        return dishes;
    }

    // ==================== 2. 一周食谱规划 ====================

    @Override
    public WeekPlanVO generateWeekPlan(WeekPlanDTO dto) {
        List<WeekPlanVO.DayPlan> days;
        if (mock) {
            days = mockWeekPlan(dto);
        } else {
            String userPrompt = loadPrompt("week_plan_prompt.txt")
                    .replace("{{servings}}", String.valueOf(dto.getServings()))
                    .replace("{{healthGoal}}", goalText(dto.getHealthGoal()))
                    .replace("{{allergy}}", orDefault(dto.getAllergy()));
            days = parseWeekPlan(aiClient.chat(SYSTEM_PROMPT, userPrompt));
        }
        // 填充每日三餐营养并汇总
        for (WeekPlanVO.DayPlan day : days) {
            fillDayNutrition(day);
        }
        WeekPlanVO vo = new WeekPlanVO();
        vo.setDays(days);
        return vo;
    }

    // ==================== 3. 替换单餐菜品 ====================

    @Override
    public AiDishVO replaceDish(ReplaceDishDTO dto) {
        AiDishVO dish;
        if (mock) {
            dish = copyDish(pickFromPool(dto.getMealType(), dto.getExcludeDishNames()),
                    dto.getServings() == null ? 1 : dto.getServings());
        } else {
            String userPrompt = loadPrompt("replace_dish_prompt.txt")
                    .replace("{{dayLabel}}", orDefault(dto.getDayLabel()))
                    .replace("{{mealType}}", mealText(dto.getMealType()))
                    .replace("{{healthGoal}}", goalText(dto.getHealthGoal()))
                    .replace("{{allergy}}", orDefault(dto.getAllergy()))
                    .replace("{{excludeList}}", dto.getExcludeDishNames() == null
                            ? "无" : String.join("、", dto.getExcludeDishNames()))
                    .replace("{{servings}}", String.valueOf(dto.getServings() == null ? 1 : dto.getServings()));
            dish = parseSingleDish(aiClient.chat(SYSTEM_PROMPT, userPrompt));
        }
        calculator.fillDishNutrition(dish);
        return dish;
    }

    // ==================== 4. 购物清单汇总 ====================

    @Override
    public ShoppingListVO buildShoppingList(ShoppingListDTO dto) {
        // key = F:食材ID(已匹配) / N:食材名(未匹配)，跨菜品累加重量
        Map<String, ShoppingListVO.ShoppingItem> itemMap = new LinkedHashMap<>();
        for (AiDishInput dish : dto.getDishes()) {
            if (dish.getIngredients() == null) {
                continue;
            }
            for (AiDishInput.Ingredient ing : dish.getIngredients()) {
                if (!StringUtils.hasText(ing.getFoodName()) || ing.getWeight() == null) {
                    continue;
                }
                FoodNutrition food = calculator.match(ing.getFoodName());
                String key = food != null ? "F:" + food.getId() : "N:" + ing.getFoodName();
                ShoppingListVO.ShoppingItem item = itemMap.get(key);
                if (item == null) {
                    item = new ShoppingListVO.ShoppingItem();
                    item.setFoodName(food != null ? food.getFoodName() : ing.getFoodName());
                    item.setWeight(BigDecimal.ZERO);
                    item.setMatched(food != null);
                    item.setSourceCategory(food != null ? food.getCategory() : null);
                    itemMap.put(key, item);
                }
                item.setWeight(item.getWeight().add(ing.getWeight()).setScale(1, RoundingMode.HALF_UP));
            }
        }
        // 按蔬菜/肉类水产/调料/其他分组
        Map<String, List<ShoppingListVO.ShoppingItem>> groups = new LinkedHashMap<>();
        SHOPPING_ORDER.forEach(g -> groups.put(g, new ArrayList<>()));
        itemMap.values().forEach(item -> groups.get(shoppingCategory(item.getSourceCategory())).add(item));
        List<ShoppingListVO.CategoryGroup> categories = new ArrayList<>();
        groups.forEach((name, items) -> {
            if (!items.isEmpty()) {
                ShoppingListVO.CategoryGroup group = new ShoppingListVO.CategoryGroup();
                group.setCategory(name);
                group.setItems(items);
                categories.add(group);
            }
        });
        ShoppingListVO vo = new ShoppingListVO();
        vo.setCategories(categories);
        return vo;
    }

    // ==================== 5. 食谱改良 ====================

    @Override
    public ImproveRecipeVO improveRecipe(ImproveRecipeDTO dto) {
        // 构建原版菜品并本地计算营养
        AiDishVO original = new AiDishVO();
        original.setDishName(dto.getDishName());
        original.setSteps(dto.getSteps());
        original.setIngredients(dto.getIngredients().stream().map(i -> {
            AiIngredientVO ing = new AiIngredientVO();
            ing.setFoodName(i.getFoodName());
            ing.setWeight(i.getWeight());
            return ing;
        }).toList());
        calculator.fillDishNutrition(original);

        // 生成改良版
        AiDishVO improved;
        if (mock) {
            improved = mockImprove(original, dto.getDirection());
        } else {
            String ingredientsText = dto.getIngredients().stream()
                    .map(i -> i.getFoodName() + i.getWeight() + "g")
                    .reduce((a, b) -> a + "、" + b).orElse("");
            String userPrompt = loadPrompt("improve_recipe_prompt.txt")
                    .replace("{{dishName}}", dto.getDishName())
                    .replace("{{ingredients}}", ingredientsText)
                    .replace("{{direction}}", directionText(dto.getDirection()));
            improved = parseSingleDish(aiClient.chat(SYSTEM_PROMPT, userPrompt));
        }
        calculator.fillDishNutrition(improved);

        // 组装对比结果(差值 = 改良版 - 原版)
        ImproveRecipeVO vo = new ImproveRecipeVO();
        vo.setOriginal(original);
        vo.setImproved(improved);
        vo.setCalorieDiff(subtract(improved.getCalorie(), original.getCalorie()));
        vo.setProteinDiff(subtract(improved.getProtein(), original.getProtein()));
        vo.setCarbohydrateDiff(subtract(improved.getCarbohydrate(), original.getCarbohydrate()));
        vo.setFatDiff(subtract(improved.getFat(), original.getFat()));
        return vo;
    }

    // ==================== Prompt模板加载与JSON解析 ====================

    /**
     * 加载prompts目录下的模板文件(带缓存)
     */
    private String loadPrompt(String fileName) {
        return promptCache.computeIfAbsent(fileName, k -> {
            try {
                return new String(new ClassPathResource("prompts/" + k).getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new BusinessException("Prompt模板读取失败: " + k);
            }
        });
    }

    /**
     * 解析AI返回的多菜品JSON: {"dishes":[{...}]}
     */
    private List<AiDishVO> parseDishes(String content) {
        try {
            JsonNode dishes = extractJson(content).path("dishes");
            List<AiDishVO> list = new ArrayList<>();
            if (dishes.isArray()) {
                for (JsonNode node : dishes) {
                    list.add(objectMapper.treeToValue(node, AiDishVO.class));
                }
            }
            if (list.isEmpty()) {
                throw new BusinessException("AI未返回有效菜品数据");
            }
            return list;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("AI返回数据解析失败");
        }
    }

    /**
     * 解析AI返回的单菜品JSON: {...}
     */
    private AiDishVO parseSingleDish(String content) {
        try {
            AiDishVO dish = objectMapper.treeToValue(extractJson(content), AiDishVO.class);
            if (dish == null || !StringUtils.hasText(dish.getDishName())) {
                throw new BusinessException("AI未返回有效菜品数据");
            }
            return dish;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("AI返回数据解析失败");
        }
    }

    /**
     * 解析一周食谱JSON并转换为日计划(营养待填充)
     */
    private List<WeekPlanVO.DayPlan> parseWeekPlan(String content) {
        try {
            JsonNode days = extractJson(content).path("days");
            List<WeekPlanVO.DayPlan> list = new ArrayList<>();
            if (days.isArray()) {
                for (JsonNode dayNode : days) {
                    WeekPlanVO.DayPlan day = new WeekPlanVO.DayPlan();
                    day.setDayLabel(dayNode.path("dayLabel").asText());
                    if (dayNode.has("breakfast")) {
                        day.setBreakfast(objectMapper.treeToValue(dayNode.path("breakfast"), AiDishVO.class));
                    }
                    if (dayNode.has("lunch")) {
                        day.setLunch(objectMapper.treeToValue(dayNode.path("lunch"), AiDishVO.class));
                    }
                    if (dayNode.has("dinner")) {
                        day.setDinner(objectMapper.treeToValue(dayNode.path("dinner"), AiDishVO.class));
                    }
                    list.add(day);
                }
            }
            if (list.isEmpty()) {
                throw new BusinessException("AI未返回有效周食谱数据");
            }
            return list;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("AI返回数据解析失败");
        }
    }

    /**
     * 从模型回复中提取JSON(剥离markdown代码块围栏与前后杂文)
     */
    private JsonNode extractJson(String content) throws IOException {
        String t = content == null ? "" : content.trim();
        if (t.startsWith("```")) {
            t = t.substring(3);
            if (t.toLowerCase().startsWith("json")) {
                t = t.substring(4);
            }
            t = t.replaceAll("```+$", "").trim();
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IOException("回复中不包含JSON");
        }
        return objectMapper.readTree(t.substring(start, end + 1));
    }

    // ==================== 通用工具方法 ====================

    /** 填充单日三餐营养并汇总当日总量 */
    private void fillDayNutrition(WeekPlanVO.DayPlan day) {
        BigDecimal calorie = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carbohydrate = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        for (AiDishVO dish : new AiDishVO[]{day.getBreakfast(), day.getLunch(), day.getDinner()}) {
            if (dish == null) {
                continue;
            }
            calculator.fillDishNutrition(dish);
            calorie = calorie.add(nvl(dish.getCalorie()));
            protein = protein.add(nvl(dish.getProtein()));
            carbohydrate = carbohydrate.add(nvl(dish.getCarbohydrate()));
            fat = fat.add(nvl(dish.getFat()));
        }
        day.setCalorie(calorie.setScale(1, RoundingMode.HALF_UP));
        day.setProtein(protein.setScale(1, RoundingMode.HALF_UP));
        day.setCarbohydrate(carbohydrate.setScale(1, RoundingMode.HALF_UP));
        day.setFat(fat.setScale(1, RoundingMode.HALF_UP));
    }

    /** 营养库分类 -> 购物清单分组映射 */
    private String shoppingCategory(String dbCategory) {
        if (dbCategory == null) {
            return "其他";
        }
        return switch (dbCategory) {
            case "蔬菜类" -> "蔬菜";
            case "肉禽蛋类", "水产类" -> "肉类水产";
            case "调料类" -> "调料";
            default -> "其他";
        };
    }

    /** 健康目标转文案 */
    private String goalText(String goal) {
        if (goal == null) {
            return "均衡维持";
        }
        return switch (goal) {
            case "LOSE" -> "减脂(控制热量)";
            case "GAIN" -> "增重(高蛋白)";
            default -> "均衡维持";
        };
    }

    /** 改良方向转文案 */
    private String directionText(String direction) {
        return switch (direction == null ? "" : direction) {
            case "LOW_CAL" -> "低卡(降低总热量)";
            case "LOW_FAT" -> "低脂(减少脂肪)";
            case "HIGH_PROTEIN" -> "高蛋白(提高蛋白质)";
            case "LOW_SUGAR" -> "控糖(减少糖与精制碳水)";
            case "LOW_SALT" -> "低盐(减少钠摄入)";
            default -> direction;
        };
    }

    /** 用餐时段转文案 */
    private String mealText(String mealType) {
        return switch (mealType == null ? "" : mealType) {
            case "BREAKFAST" -> "早餐";
            case "LUNCH" -> "午餐";
            default -> "晚餐";
        };
    }

    /** 空值兜底为"无" */
    private String orDefault(String s) {
        return StringUtils.hasText(s) ? s : "无";
    }

    /** 减法(空值按0) */
    private BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return nvl(a).subtract(nvl(b)).setScale(1, RoundingMode.HALF_UP);
    }

    /** null安全取值 */
    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // ==================== 本地模拟模式(ai.mock=true, 联调用) ====================
    // 说明: 以下模板的食材名称均与本地营养库精确一致，保证营养计算链路与真实模式一致

    /** 构建菜品对象的便捷方法 */
    private AiDishVO dish(String name, String desc, int difficulty, int minutes, List<String> steps,
                          Object[]... ingredients) {
        AiDishVO d = new AiDishVO();
        d.setDishName(name);
        d.setDescription(desc);
        d.setDifficulty(difficulty);
        d.setCookingTime(minutes);
        d.setSteps(steps);
        List<AiIngredientVO> list = new ArrayList<>();
        for (Object[] pair : ingredients) {
            AiIngredientVO ing = new AiIngredientVO();
            ing.setFoodName((String) pair[0]);
            ing.setWeight(BigDecimal.valueOf((Double) pair[1]));
            list.add(ing);
        }
        d.setIngredients(list);
        return d;
    }

    /** 食材与重量对 */
    private Object[] ing(String name, double weight) {
        return new Object[]{name, weight};
    }

    /** 模拟: 根据用户食材生成菜品 */
    private List<AiDishVO> mockGenerateDishes(GenerateRecipeDTO dto) {
        List<String> ins = (dto.getIngredients() == null || dto.getIngredients().isEmpty())
                ? List.of("鸡胸肉", "西兰花") : dto.getIngredients();
        String p1 = ins.get(0);
        String p2 = ins.size() > 1 ? ins.get(1) : "胡萝卜";
        int count = 3 + RANDOM.nextInt(3);
        List<AiDishVO> list = new ArrayList<>();
        list.add(dish("家常" + p1 + "小炒" + p2,
                "结合" + goalText(dto.getHealthGoal()) + "目标的家常小炒，营养均衡易上手",
                2, 20,
                List.of("食材洗净切配，控干水分", "热锅少油，大火快炒食材至断生", "调入食盐翻炒均匀即可出锅"),
                ing(p1, 200), ing(p2, 150), ing("花生油", 10), ing("食盐", 3)));
        list.add(dish(p1 + "营养汤品",
                "清淡少油的炖汤做法，适合" + goalText(dto.getHealthGoal()) + "人群",
                1, 25,
                List.of("食材切块焯水去腥", "加足量清水大火烧开转小火炖20分钟", "出锅前食盐调味"),
                ing(p1, 120), ing("冬瓜", 150), ing("食盐", 2)));
        list.add(dish(p1 + "盖浇饭",
                "一餐完成主食与蛋白质搭配的中式盖饭",
                1, 15,
                List.of("粳米蒸熟备用", "食材切片炒制，勾薄芡浇在米饭上", "撒葱花装盘"),
                ing("粳米(标准)", 150), ing(p1, 100), ing("黄瓜", 50)));
        if (count >= 4) {
            list.add(dish("清蒸" + p1,
                    "最大限度保留食材本味与营养的低油做法",
                    1, 15,
                    List.of("食材摆盘，铺姜丝去腥", "水开后上锅大火蒸10分钟", "淋生抽与热油激香即可"),
                    ing(p1, 250), ing("生抽", 5), ing("花生油", 5)));
        }
        if (count >= 5) {
            list.add(dish("凉拌" + p2,
                    "清爽开胃的凉拌菜，几乎不增加热量负担",
                    1, 10,
                    List.of("食材切丝焯水过凉", "加生抽、香油拌匀", "冷藏10分钟口感更佳"),
                    ing(p2, 200), ing("芝麻香油", 3), ing("生抽", 5)));
        }
        return list;
    }

    /** 模拟: 早餐模板池 */
    private List<AiDishVO> breakfastPool() {
        return List.of(
                dish("营养牛奶套餐", "优质蛋白与全麦碳水的经典早餐组合", 1, 10,
                        List.of("牛奶隔水加热至温热", "鸡蛋冷水下锅煮8分钟", "全麦面包烤箱加热后装盘"),
                        ing("牛奶(全脂)", 250), ing("全麦面包", 70), ing("鸡蛋(均值)", 50)),
                dish("养胃小米粥套餐", "温和易消化的传统中式早餐", 1, 15,
                        List.of("小米粥加热", "鸡蛋煮熟剥壳", "花卷上锅蒸5分钟"),
                        ing("小米粥", 300), ing("鸡蛋(均值)", 50), ing("花卷", 80)),
                dish("燕麦香蕉杯", "高纤维饱腹感强的西式轻早餐", 1, 5,
                        List.of("燕麦片用牛奶冲泡", "香蕉切片铺在表面", "静置2分钟软化后食用"),
                        ing("燕麦片", 45), ing("牛奶(全脂)", 200), ing("香蕉", 100)),
                dish("经典豆浆餐", "植物蛋白搭配传统面食", 1, 10,
                        List.of("豆浆加热至沸腾", "馒头切片烤至微黄", "黄瓜切段佐餐"),
                        ing("豆浆(无糖)", 300), ing("馒头(均值)", 100), ing("黄瓜", 80)),
                dish("玉米鸡蛋餐", "粗粮与优质蛋白的简易组合", 1, 15,
                        List.of("玉米切段水煮10分钟", "鸡蛋煮熟", "豆浆加热佐餐"),
                        ing("玉米(鲜)", 200), ing("鸡蛋(均值)", 60), ing("豆浆(无糖)", 250)));
    }

    /** 模拟: 午餐模板池 */
    private List<AiDishVO> lunchPool() {
        return List.of(
                dish("香煎鸡胸杂粮饭", "高蛋白低脂肪的健身午餐", 2, 25,
                        List.of("糙米提前浸泡后蒸熟", "鸡胸肉少油煎至两面金黄", "西兰花焯水摆盘"),
                        ing("糙米", 75), ing("鸡胸肉", 130), ing("西兰花", 150), ing("花生油", 8)),
                dish("番茄牛肉盖饭", "经典中式盖饭，酸甜开胃", 2, 30,
                        List.of("牛肉切片腌制10分钟", "番茄炒出汁后下牛肉滑炒", "浇在米饭上撒葱花"),
                        ing("粳米(标准)", 75), ing("牛肉(瘦)", 110), ing("番茄", 160), ing("花生油", 8)),
                dish("菠菜瘦肉餐", "补铁补蛋白的家常搭配", 2, 20,
                        List.of("瘦肉切丝滑油盛出", "菠菜大火快炒后回锅肉丝", "配米饭食用"),
                        ing("粳米(标准)", 75), ing("猪肉(瘦)", 90), ing("菠菜", 180), ing("花生油", 8)),
                dish("香煎三文鱼藜麦餐", "优质脂肪与完整蛋白的组合", 2, 20,
                        List.of("藜麦煮熟沥干", "三文鱼橄榄油小火煎至两面变色", "芦笋焯水摆盘"),
                        ing("藜麦", 60), ing("三文鱼", 120), ing("芦笋", 150), ing("橄榄油", 6)),
                dish("家常豆腐套餐", "植物蛋白丰富的人家常菜", 2, 20,
                        List.of("豆腐切块煎至两面金黄", "油菜清炒垫盘", "豆腐回锅红烧收汁"),
                        ing("糙米", 75), ing("豆腐(北)", 150), ing("油菜", 150), ing("花生油", 10)),
                dish("清蒸鲈鱼套餐", "低脂高蛋白的清淡粤式做法", 2, 25,
                        List.of("鲈鱼改刀铺姜葱", "水开后大火蒸8分钟", "淋生抽热油，配米饭"),
                        ing("粳米(标准)", 75), ing("鲈鱼", 180), ing("生菜", 150), ing("花生油", 6)),
                dish("虾仁冬瓜套餐", "低卡清爽的家常组合", 2, 20,
                        List.of("虾仁焯水备用", "冬瓜片炒软后下虾仁", "配米饭出锅"),
                        ing("粳米(标准)", 75), ing("虾仁(生)", 120), ing("冬瓜", 200), ing("胡萝卜", 50), ing("花生油", 8)));
    }

    /** 模拟: 晚餐模板池 */
    private List<AiDishVO> dinnerPool() {
        return List.of(
                dish("轻食沙拉晚餐", "低热量高纤维的晚间轻食", 1, 10,
                        List.of("蔬菜洗净撕成适口大小", "鸡胸肉煎熟切片", "淋橄榄油拌匀"),
                        ing("生菜", 150), ing("鸡胸肉", 80), ing("番茄", 100), ing("吐司(切片)", 50), ing("橄榄油", 5)),
                dish("蔬菜蛋花粥", "暖胃易消化的晚间粥品", 1, 15,
                        List.of("大米粥加热至沸腾", "菠菜切段下锅", "淋入蛋液搅出蛋花"),
                        ing("大米粥", 250), ing("菠菜", 150), ing("鸡蛋(均值)", 50), ing("花生油", 5)),
                dish("蒸红薯配牛奶", "粗粮代餐的简单晚餐", 1, 20,
                        List.of("红薯洗净蒸20分钟", "牛奶加热佐餐", "黄瓜切段清口"),
                        ing("红薯(红心)", 200), ing("牛奶(全脂)", 200), ing("黄瓜", 100)),
                dish("冬瓜豆腐汤套餐", "低卡饱腹的汤粥组合", 1, 15,
                        List.of("冬瓜与豆腐切块", "清水煮10分钟至透明", "食盐调味配粥食用"),
                        ing("冬瓜", 200), ing("豆腐(北)", 100), ing("大米粥", 250), ing("花生油", 4)),
                dish("黄金蛋炒饭", "利用剩饭的快手中式晚餐", 2, 10,
                        List.of("鸡蛋炒散盛出", "米饭下锅压散翻炒", "胡萝卜豌豆与鸡蛋回锅炒匀"),
                        ing("粳米(标准)", 70), ing("鸡蛋(均值)", 60), ing("胡萝卜", 50), ing("豌豆(鲜)", 50), ing("花生油", 8)),
                dish("山药炖鸡汤", "温补养胃的炖汤晚餐", 2, 40,
                        List.of("鸡腿焯水去血沫", "山药段下锅同炖30分钟", "食盐调味出锅"),
                        ing("山药", 150), ing("鸡腿", 120), ing("玉米(鲜)", 100), ing("花生油", 5)),
                dish("凉拌豆皮黄瓜", "清爽低卡的凉拌晚餐", 1, 10,
                        List.of("豆腐皮切丝焯水", "黄瓜拍段", "加生抽香油拌匀"),
                        ing("黄瓜", 150), ing("豆腐皮", 30), ing("大米粥", 250), ing("芝麻香油", 3)));
    }

    /** 模拟: 一周计划组装 */
    private List<WeekPlanVO.DayPlan> mockWeekPlan(WeekPlanDTO dto) {
        int servings = dto.getServings() == null ? 1 : Math.max(1, dto.getServings());
        List<AiDishVO> bf = breakfastPool();
        List<AiDishVO> ln = lunchPool();
        List<AiDishVO> dn = dinnerPool();
        List<WeekPlanVO.DayPlan> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            WeekPlanVO.DayPlan day = new WeekPlanVO.DayPlan();
            day.setDayLabel(DAY_LABELS[i]);
            day.setBreakfast(copyDish(bf.get(i % bf.size()), servings));
            day.setLunch(copyDish(ln.get(i % ln.size()), servings));
            day.setDinner(copyDish(dn.get(i % dn.size()), servings));
            days.add(day);
        }
        return days;
    }

    /** 模拟: 按时段从模板池选择非重复菜品 */
    private AiDishVO pickFromPool(String mealType, List<String> excludeDishNames) {
        List<AiDishVO> pool = switch (mealType == null ? "" : mealType) {
            case "BREAKFAST" -> breakfastPool();
            case "LUNCH" -> lunchPool();
            default -> dinnerPool();
        };
        return pool.stream()
                .filter(d -> excludeDishNames == null || !excludeDishNames.contains(d.getDishName()))
                .findFirst()
                .orElse(pool.get(0));
    }

    /** 深拷贝菜品并按人数放大食材重量 */
    private AiDishVO copyDish(AiDishVO src, int servings) {
        AiDishVO d = new AiDishVO();
        d.setDishName(src.getDishName());
        d.setDescription(src.getDescription());
        d.setDifficulty(src.getDifficulty());
        d.setCookingTime(src.getCookingTime());
        d.setSteps(src.getSteps());
        List<AiIngredientVO> list = new ArrayList<>();
        for (AiIngredientVO srcIng : src.getIngredients()) {
            AiIngredientVO ing = new AiIngredientVO();
            ing.setFoodName(srcIng.getFoodName());
            ing.setWeight(srcIng.getWeight()
                    .multiply(BigDecimal.valueOf(servings))
                    .setScale(1, RoundingMode.HALF_UP));
            list.add(ing);
        }
        d.setIngredients(list);
        return d;
    }

    /** 模拟: 按方向对原菜品做食材替换/用量调整生成改良版 */
    private AiDishVO mockImprove(AiDishVO original, String direction) {
        AiDishVO improved = new AiDishVO();
        improved.setDishName(original.getDishName() + "(改良版)");
        improved.setDescription("按" + directionText(direction) + "方向调整食材与用量，保持中式做法不变");
        improved.setDifficulty(original.getDifficulty());
        improved.setCookingTime(original.getCookingTime());
        improved.setSteps(original.getSteps());
        List<AiIngredientVO> out = new ArrayList<>();
        boolean hasMeat = false;
        for (AiIngredientVO src : original.getIngredients()) {
            AiIngredientVO ing = new AiIngredientVO();
            ing.setFoodName(src.getFoodName());
            ing.setWeight(src.getWeight());
            String name = src.getFoodName() == null ? "" : src.getFoodName();
            switch (direction == null ? "" : direction) {
                case "LOW_CAL", "LOW_FAT" -> {
                    // 减半用油与糖，五花肉等高脂肉替换为鸡胸肉
                    if (name.contains("油") || name.contains("糖")) {
                        ing.setWeight(scale(src.getWeight(), "0.5"));
                    }
                    if (name.contains("五花") || "猪肉(肥瘦)".equals(name)) {
                        ing.setFoodName("鸡胸肉");
                    }
                }
                case "HIGH_PROTEIN" -> {
                    if (isMeatName(name)) {
                        ing.setWeight(scale(src.getWeight(), "1.3"));
                        hasMeat = true;
                    }
                }
                case "LOW_SUGAR" -> {
                    if (name.contains("糖")) {
                        ing.setWeight(scale(src.getWeight(), "0.3"));
                    }
                    if (isStapleName(name)) {
                        ing.setWeight(scale(src.getWeight(), "0.9"));
                    }
                }
                case "LOW_SALT" -> {
                    if (name.contains("盐") || name.contains("酱油") || name.contains("酱")) {
                        ing.setWeight(scale(src.getWeight(), "0.4"));
                    }
                }
                default -> {
                }
            }
            if (isMeatName(ing.getFoodName())) {
                hasMeat = true;
            }
            out.add(ing);
        }
        // 高蛋白方向: 原菜品无肉类时补充鸡胸肉
        if ("HIGH_PROTEIN".equals(direction) && !hasMeat) {
            AiIngredientVO extra = new AiIngredientVO();
            extra.setFoodName("鸡胸肉");
            extra.setWeight(new BigDecimal("100"));
            out.add(extra);
        }
        improved.setIngredients(out);
        return improved;
    }

    /** 合并用户档案忌口与入参忌口 */
    private String effectiveAllergy(String dtoAllergy) {
        try {
            var user = userService.getById(currentUser());
            String userAllergy = user != null ? user.getAllergy() : null;
            if (userAllergy == null || userAllergy.isBlank()) {
                return dtoAllergy;
            }
            if (dtoAllergy == null || dtoAllergy.isBlank()) {
                return userAllergy;
            }
            return userAllergy + "," + dtoAllergy;
        } catch (Exception e) {
            return dtoAllergy;
        }
    }

    /** 当前登录用户ID(无上下文时返回null) */
    private Long currentUser() {
        var info = com.diet.common.UserContext.get();
        return info == null ? null : info.getUserId();
    }

    /**
     * 忌口过滤: 将菜品中含忌口关键词的食材替换为安全食材
     * (用户档案与请求中的忌口合并生效, 生成食谱时自动过滤)
     */
    private void applyAllergyFilter(List<AiDishVO> dishes, String allergy) {
        if (allergy == null || allergy.isBlank()) {
            return;
        }
        // 忌口关键词列表(按逗号/分号切分)
        List<String> keys = Arrays.stream(allergy.split("[,，;；]"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (keys.isEmpty()) {
            return;
        }
        // 安全替换池(依次选择不含忌口词的食材)
        List<String> safeFoods = List.of("鸡胸肉", "豆腐(北)", "冬瓜", "西兰花", "土豆", "白菜");
        for (AiDishVO dish : dishes) {
            if (dish.getIngredients() == null) {
                continue;
            }
            for (AiIngredientVO ing : dish.getIngredients()) {
                String name = ing.getFoodName() == null ? "" : ing.getFoodName();
                if (isAllergyHit(name, keys)) {
                    // 替换为第一个不含忌口词的安全食材
                    String safe = safeFoods.stream()
                            .filter(s -> keys.stream().noneMatch(s::contains))
                            .findFirst().orElse("西兰花");
                    ing.setFoodName(safe);
                }
            }
        }
    }

    /**
     * 判断食材是否命中忌口:
     * 1. 关键词子串匹配(如"花生"命中"花生油")
     * 2. 分类级匹配(如忌口"海鲜"命中全部水产类, "坚果"命中坚果类)
     */
    private boolean isAllergyHit(String name, List<String> keys) {
        if (keys.stream().anyMatch(name::contains)) {
            return true;
        }
        // 分类级扩展匹配
        var food = calculator.match(name);
        if (food == null) {
            return false;
        }
        String category = food.getCategory();
        return keys.stream().anyMatch(k -> switch (k) {
            case "海鲜", "水产" -> "水产类".equals(category);
            case "坚果" -> "坚果类".equals(category);
            case "蛋", "鸡蛋" -> "肉禽蛋类".equals(category) && name.contains("蛋");
            case "奶", "乳制品" -> "奶豆类".equals(category) && (name.contains("奶") || name.contains("乳"));
            default -> false;
        });
    }

    /** 重量按比例缩放 */
    private BigDecimal scale(BigDecimal weight, String factor) {
        return weight.multiply(new BigDecimal(factor)).setScale(1, RoundingMode.HALF_UP);
    }

    /** 是否肉类食材(粗略关键词判断) */
    private boolean isMeatName(String name) {
        return name != null && Arrays.stream(new String[]{"肉", "鱼", "虾", "蟹", "贝", "鸡腿", "鸡胸", "牛肉", "羊肉", "排骨"})
                .anyMatch(name::contains);
    }

    /** 是否主食类食材 */
    private boolean isStapleName(String name) {
        return name != null && Arrays.stream(new String[]{"米", "面", "粉", "面包", "馒头", "粥"})
                .anyMatch(name::contains);
    }
}