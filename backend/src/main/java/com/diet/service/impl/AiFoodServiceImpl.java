package com.diet.service.impl;

import com.diet.common.BusinessException;
import com.diet.dto.AiFoodParseDTO;
import com.diet.dto.AiFoodParseVO;
import com.diet.dto.AiIngredientVO;
import com.diet.entity.FoodNutrition;
import com.diet.service.AiFoodService;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI食物营养解析服务实现类
 *
 * 流程: 用户输入食物/菜名 -> AI解析为食材名称+克数 -> NutritionCalculator基于本地营养库计算营养
 * 硬性约束: AI不输出任何营养数值，全部由本地1010条营养库换算
 *
 * @author diet
 */
@Service
public class AiFoodServiceImpl implements AiFoodService {

    /** 系统提示词 */
    private static final String SYSTEM_PROMPT =
            "你是一名中式营养配餐师，擅长将菜品拆解为具体食材组成。严格按JSON格式输出，禁止输出热量数值与JSON以外的文字。";

    /** Prompt模板缓存 */
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    private final AiClient aiClient;
    private final NutritionCalculator calculator;
    private final ObjectMapper objectMapper;

    /** 是否本地模拟模式 */
    @Value("${ai.mock:true}")
    private boolean mock;

    public AiFoodServiceImpl(AiClient aiClient, NutritionCalculator calculator, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.calculator = calculator;
        this.objectMapper = objectMapper;
    }

    /**
     * 解析食物: AI输出食材组成，本地库计算营养并汇总
     */
    @Override
    public AiFoodParseVO parseFood(AiFoodParseDTO dto) {
        List<AiIngredientVO> ingredients;
        String displayName = dto.getFoodName().trim();
        BigDecimal servings = dto.getServings() == null || dto.getServings().compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE : dto.getServings();
        if (mock) {
            ingredients = mockParse(displayName);
            // 模拟模式同样按份数放大食材重量
            BigDecimal s = servings;
            ingredients.forEach(ing -> ing.setWeight(ing.getWeight().multiply(s)
                    .setScale(1, RoundingMode.HALF_UP)));
        } else {
            // 组装Prompt(份数提示)
            String userPrompt = loadPrompt("food_parse_prompt.txt")
                    .replace("{{foodName}}", displayName)
                    .replace("{{hint}}", servings.stripTrailingZeros().toPlainString() + "份(按倍数放大食材重量)");
            ingredients = parseIngredients(aiClient.chat(SYSTEM_PROMPT, userPrompt));
            // 按份数放大重量
            ingredients.forEach(ing -> ing.setWeight(ing.getWeight().multiply(servings)
                    .setScale(1, RoundingMode.HALF_UP)));
        }
        if (ingredients.isEmpty()) {
            throw new BusinessException("无法解析该食物，请尝试更具体的名称(如: 番茄炒蛋、牛肉面)");
        }
        // 本地营养库逐项计算
        ingredients.forEach(this::fillIngredient);
        // 汇总
        AiFoodParseVO vo = new AiFoodParseVO();
        vo.setFoodName(displayName);
        vo.setIngredients(ingredients);
        BigDecimal calorie = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carb = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        int unmatched = 0;
        for (AiIngredientVO ing : ingredients) {
            if (Boolean.FALSE.equals(ing.getMatched())) {
                unmatched++;
                continue;
            }
            calorie = calorie.add(nvl(ing.getCalorie()));
            protein = protein.add(nvl(ing.getProtein()));
            carb = carb.add(nvl(ing.getCarbohydrate()));
            fat = fat.add(nvl(ing.getFat()));
        }
        vo.setTotalCalorie(calorie.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalProtein(protein.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalCarbohydrate(carb.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalFat(fat.setScale(1, RoundingMode.HALF_UP));
        vo.setUnmatchedCount(unmatched);
        return vo;
    }

    /**
     * 单项食材营养计算(匹配本地库 -> 按重量/100换算)
     */
    private void fillIngredient(AiIngredientVO ing) {
        if (ing.getWeight() == null) {
            ing.setWeight(BigDecimal.ZERO);
        }
        FoodNutrition food = calculator.match(ing.getFoodName());
        if (food == null) {
            ing.setMatched(false);
            return;
        }
        BigDecimal factor = ing.getWeight().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        ing.setFoodId(food.getId());
        ing.setMatched(true);
        ing.setCategory(food.getCategory());
        ing.setCalorie(multiply(food.getCalorie(), factor));
        ing.setProtein(multiply(food.getProtein(), factor));
        ing.setCarbohydrate(multiply(food.getCarbohydrate(), factor));
        ing.setFat(multiply(food.getFat(), factor));
    }

    /**
     * 解析AI返回的食材JSON: {"foodName":"...","ingredients":[{...}]}
     */
    private List<AiIngredientVO> parseIngredients(String content) {
        try {
            String t = content == null ? "" : content.trim();
            // 剥离markdown围栏
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
                throw new IOException("无JSON");
            }
            JsonNode root = objectMapper.readTree(t.substring(start, end + 1));
            List<AiIngredientVO> list = new ArrayList<>();
            JsonNode arr = root.path("ingredients");
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    AiIngredientVO ing = new AiIngredientVO();
                    ing.setFoodName(node.path("foodName").asText());
                    ing.setWeight(node.path("weight").decimalValue());
                    if (StringUtils.hasText(ing.getFoodName()) && ing.getWeight() != null) {
                        list.add(ing);
                    }
                }
            }
            return list;
        } catch (Exception e) {
            throw new BusinessException("AI返回数据解析失败，请重试");
        }
    }

    /**
     * 本地模拟解析(联调用):
     * 1. 名称直接命中本地库 -> 单食材
     * 2. 否则按"主料+常见配料"规则组装: 主料(尝试词缀匹配)+蔬菜+油+盐
     */
    private List<AiIngredientVO> mockParse(String name) {
        List<AiIngredientVO> list = new ArrayList<>();
        // 1. 直接命中营养库
        FoodNutrition direct = calculator.match(name);
        if (direct != null && direct.getFoodName().equals(name)) {
            AiIngredientVO ing = new AiIngredientVO();
            ing.setFoodName(direct.getFoodName());
            ing.setWeight(new BigDecimal("100"));
            list.add(ing);
            return list;
        }
        // 2. 词缀尝试: "鸡蛋羹"->"鸡蛋"、"炒青菜"->"青菜"
        FoodNutrition prefix = calculator.match(name.replaceAll("[羹汤饼面条饭包子饺子粥炒烧蒸煮煎烤]$|^炒|^烧", ""));
        if (prefix != null) {
            AiIngredientVO main = new AiIngredientVO();
            main.setFoodName(prefix.getFoodName());
            main.setWeight(new BigDecimal("150"));
            list.add(main);
            // 菜品类默认补充油盐
            AiIngredientVO oil = new AiIngredientVO();
            oil.setFoodName("花生油");
            oil.setWeight(new BigDecimal("10"));
            list.add(oil);
            AiIngredientVO salt = new AiIngredientVO();
            salt.setFoodName("食盐");
            salt.setWeight(new BigDecimal("2"));
            list.add(salt);
            return list;
        }
        // 3. 兜底: 无法识别的菜品给出基础估算组合
        AiIngredientVO staple = new AiIngredientVO();
        staple.setFoodName("粳米(标准)");
        staple.setWeight(new BigDecimal("100"));
        list.add(staple);
        AiIngredientVO veg = new AiIngredientVO();
        veg.setFoodName("大白菜");
        veg.setWeight(new BigDecimal("150"));
        list.add(veg);
        AiIngredientVO protein = new AiIngredientVO();
        protein.setFoodName("鸡蛋(均值)");
        protein.setWeight(new BigDecimal("50"));
        list.add(protein);
        AiIngredientVO oil = new AiIngredientVO();
        oil.setFoodName("花生油");
        oil.setWeight(new BigDecimal("8"));
        list.add(oil);
        AiIngredientVO salt = new AiIngredientVO();
        salt.setFoodName("食盐");
        salt.setWeight(new BigDecimal("2"));
        list.add(salt);
        return list;
    }

    /** 加载Prompt模板 */
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

    /** 营养值*系数保留1位 */
    private BigDecimal multiply(BigDecimal base, BigDecimal factor) {
        return base.multiply(factor).setScale(1, RoundingMode.HALF_UP);
    }

    /** null安全 */
    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}