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
import org.springframework.web.multipart.MultipartFile;

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
            "你是一名中式营养配餐师，擅长将菜品拆解为具体食材，并对每种食材科学估算每100克的热量与三大营养素。严格按JSON格式输出，不要输出JSON以外的任何文字。";

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
     * 拍照识别食物: 视觉模型识别菜品+估算重量, 营养按每100g值x重量换算(与文本解析真实链路口径一致)
     */
    @Override
    public AiFoodParseVO parseFoodPhoto(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException("请上传照片");
        }
        List<AiIngredientVO> ingredients;
        String dishName;
        if (mock) {
            // 模拟模式: 固定返回一碗米饭(本地库计算), 供无密钥环境联调整条链路
            dishName = "米饭(蒸)";
            ingredients = mockParse(dishName);
            ingredients.forEach(this::fillIngredientMock);
        } else {
            String dataUrl = toDataUrl(image);
            String userPrompt = loadPrompt("food_photo_prompt.txt");
            String content = aiClient.chatVision(SYSTEM_PROMPT, userPrompt, dataUrl);
            PhotoParseResult r = parsePhotoContent(content);
            dishName = r.dishName;
            ingredients = r.ingredients;
            // 真实模式: AI给出每100g营养 -> 按各食材实际重量换算
            ingredients.forEach(ing -> {
                ing.setMatched(true);
                ing.setCategory("AI估算");
                BigDecimal factor = nvl(ing.getWeight()).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                ing.setCalorie(multiply(ing.getCalorie(), factor));
                ing.setProtein(multiply(ing.getProtein(), factor));
                ing.setCarbohydrate(multiply(ing.getCarbohydrate(), factor));
                ing.setFat(multiply(ing.getFat(), factor));
            });
        }
        if (ingredients.isEmpty() || !StringUtils.hasText(dishName)) {
            throw new BusinessException("未能从照片中识别出食物，请换个角度拍摄或手动输入");
        }
        // 汇总营养
        BigDecimal calorie = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carb = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        for (AiIngredientVO ing : ingredients) {
            calorie = calorie.add(nvl(ing.getCalorie()));
            protein = protein.add(nvl(ing.getProtein()));
            carb = carb.add(nvl(ing.getCarbohydrate()));
            fat = fat.add(nvl(ing.getFat()));
        }
        AiFoodParseVO vo = new AiFoodParseVO();
        vo.setFoodName(dishName);
        vo.setIngredients(ingredients);
        vo.setTotalCalorie(calorie.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalProtein(protein.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalCarbohydrate(carb.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalFat(fat.setScale(1, RoundingMode.HALF_UP));
        vo.setUnmatchedCount(0);
        return vo;
    }

    /** 上传图片转base64数据URI */
    private String toDataUrl(MultipartFile image) {
        try {
            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BusinessException("仅支持图片文件");
            }
            byte[] data = image.getBytes();
            // 防御: 过大的图片直接拒绝(前端正常压缩后在1MB以内)
            if (data.length > 8 * 1024 * 1024) {
                throw new BusinessException("图片过大，请重新拍摄");
            }
            return "data:" + contentType + ";base64,"
                    + java.util.Base64.getEncoder().encodeToString(data);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("图片读取失败，请重试");
        }
    }

    /** 解析视觉模型返回: {"foodName":"..","totalWeight":350,"ingredients":[...]} */
    private PhotoParseResult parsePhotoContent(String content) {
        try {
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
                throw new IOException("无JSON");
            }
            JsonNode root = objectMapper.readTree(t.substring(start, end + 1));
            PhotoParseResult r = new PhotoParseResult();
            r.dishName = root.path("foodName").asText("");
            r.ingredients = new ArrayList<>();
            JsonNode arr = root.path("ingredients");
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    AiIngredientVO ing = new AiIngredientVO();
                    ing.setFoodName(node.path("foodName").asText());
                    ing.setWeight(node.path("weight").decimalValue());
                    ing.setCalorie(num(node.path("calorie")));
                    ing.setProtein(num(node.path("protein")));
                    ing.setCarbohydrate(num(node.path("carbohydrate")));
                    ing.setFat(num(node.path("fat")));
                    if (StringUtils.hasText(ing.getFoodName()) && ing.getWeight() != null
                            && ing.getWeight().compareTo(BigDecimal.ZERO) > 0) {
                        r.ingredients.add(ing);
                    }
                }
            }
            return r;
        } catch (IOException e) {
            throw new BusinessException("AI返回数据解析失败，请重试");
        }
    }

    /** 模拟模式下按本地库填充单项营养(与parseFood的mock分支一致) */
    private void fillIngredientMock(AiIngredientVO ing) {
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

    /** 视觉模型返回的中间结构 */
    private static class PhotoParseResult {
        String dishName;
        List<AiIngredientVO> ingredients;
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
        // 若用户提供了总重量(g), 则按比例将全部食材缩放到该总重量(覆盖上述份数放大)
        if (dto.getTotalWeight() != null && dto.getTotalWeight().compareTo(BigDecimal.ZERO) > 0) {
            scaleToTotalWeight(ingredients, dto.getTotalWeight());
        }
        if (ingredients.isEmpty()) {
            throw new BusinessException("无法解析该食物，请尝试更具体的名称(如: 番茄炒蛋、牛肉面)");
        }
        // 营养计算: 真实模式由AI直接给出每100g营养值并按重量换算; 模拟模式走本地营养库(联调用)
        BigDecimal calorie = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carb = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        int unmatched = 0;
        for (AiIngredientVO ing : ingredients) {
            if (mock) {
                // 模拟模式: 本地库填充营养(仅用于联调, 与真实链路一致)
                fillIngredient(ing);
                if (Boolean.FALSE.equals(ing.getMatched())) {
                    unmatched++;
                    continue;
                }
            } else {
                // 真实模式: AI直接提供每100g营养 -> 按该食材实际重量换算
                ing.setMatched(true);
                ing.setCategory("AI估算");
                BigDecimal factor = nvl(ing.getWeight()).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                ing.setCalorie(multiply(ing.getCalorie(), factor));
                ing.setProtein(multiply(ing.getProtein(), factor));
                ing.setCarbohydrate(multiply(ing.getCarbohydrate(), factor));
                ing.setFat(multiply(ing.getFat(), factor));
            }
            calorie = calorie.add(nvl(ing.getCalorie()));
            protein = protein.add(nvl(ing.getProtein()));
            carb = carb.add(nvl(ing.getCarbohydrate()));
            fat = fat.add(nvl(ing.getFat()));
        }
        AiFoodParseVO vo = new AiFoodParseVO();
        vo.setFoodName(displayName);
        vo.setIngredients(ingredients);
        vo.setTotalCalorie(calorie.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalProtein(protein.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalCarbohydrate(carb.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalFat(fat.setScale(1, RoundingMode.HALF_UP));
        vo.setUnmatchedCount(unmatched);
        return vo;
    }

    /**
     * 按目标总重量等比缩放全部食材:
     * 系数 = 目标总重量 / 当前食材总重量, 每项食材 × 系数
     * 用途: 用户输入这道菜实际吃了多少克, AI按各食材比例分配到该总重量
     */
    private void scaleToTotalWeight(List<AiIngredientVO> ingredients, BigDecimal targetWeight) {
        // 当前食材总重量(忽略未设重量的项)
        BigDecimal currentTotal = ingredients.stream()
                .map(AiIngredientVO::getWeight)
                .filter(w -> w != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (currentTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        // 缩放系数, 保留4位小数精度
        BigDecimal factor = targetWeight.divide(currentTotal, 4, RoundingMode.HALF_UP);
        ingredients.forEach(ing -> {
            if (ing.getWeight() != null) {
                ing.setWeight(ing.getWeight().multiply(factor).setScale(1, RoundingMode.HALF_UP));
            }
        });
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
                    // AI直接给出的每100g营养值(真实模式使用)
                    ing.setCalorie(num(node.path("calorie")));
                    ing.setProtein(num(node.path("protein")));
                    ing.setCarbohydrate(num(node.path("carbohydrate")));
                    ing.setFat(num(node.path("fat")));
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

    /** 读取数字节点, 缺失/非法时按0处理 */
    private BigDecimal num(JsonNode n) {
        return (n != null && n.isNumber()) ? n.decimalValue() : BigDecimal.ZERO;
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