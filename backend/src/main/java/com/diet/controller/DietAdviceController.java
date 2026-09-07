package com.diet.controller;

import com.diet.common.BusinessException;
import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.dto.DailySummaryVO;
import com.diet.dto.HealthCalcVO;
import com.diet.entity.SysUser;
import com.diet.service.HealthCalcService;
import com.diet.service.UserService;
import com.diet.util.AiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI饮食建议控制器
 * 每日对比实际摄入与推荐标准，由大模型生成自然语言调整建议
 * (营养数值仍由本地计算得出, 大模型仅负责结合数据生成个性化文案)
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/health")
public class DietAdviceController {

    /** 系统提示词: 营养师角色与输出约束 */
    private static final String SYSTEM_PROMPT =
            "你是一名专业注册营养师，结合用户每日实际摄入与推荐值生成简短亲切的中式饮食调整建议，"
                    + "禁止给出医疗诊断或药物建议，回复必须以「仅供饮食参考，不构成医疗建议。」结尾。";

    /** Prompt模板缓存 */
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    private final HealthCalcService healthCalcService;
    private final UserService userService;
    private final AiClient aiClient;

    /** 是否本地模拟模式(未配置真实Key时走规则引擎联调) */
    @Value("${ai.mock:true}")
    private boolean mock;

    public DietAdviceController(HealthCalcService healthCalcService, UserService userService, AiClient aiClient) {
        this.healthCalcService = healthCalcService;
        this.userService = userService;
        this.aiClient = aiClient;
    }

    /**
     * 每日AI饮食建议: 对比当日实际摄入与推荐值生成自然语言调整建议
     *
     * @param date 日期, 默认今天
     */
    @GetMapping("/daily-advice")
    public Result<Map<String, Object>> dailyAdvice(@RequestParam(required = false) String date) {
        Long userId = UserContext.get().getUserId();
        LocalDate d = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        DailySummaryVO summary = healthCalcService.dailySummary(userId, d);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", summary.getDate());
        // 未完善档案: 无推荐值可对比, 直接返回提示(不调用AI)
        if (summary.getRecommendCalorie() == null) {
            result.put("advice", "您还未完善健康档案，请先在健康档案页填写身高、体重等信息，获取个性化推荐摄入量。"
                    + "仅供饮食参考，不构成医疗建议。");
            result.put("summary", summary);
            return Result.success(result);
        }

        String advice;
        if (mock || !aiClient.isConfigured()) {
            advice = buildRuleAdvice(summary);
        } else {
            String userPrompt = loadPrompt("diet_advice_prompt.txt")
                    .replace("{{healthProfile}}", buildHealthProfile(userId))
                    .replace("{{dailyIntake}}", buildDailyIntake(summary))
                    .replace("{{comparison}}", buildComparison(summary));
            advice = aiClient.chat(SYSTEM_PROMPT, userPrompt);
        }
        result.put("advice", advice);
        result.put("summary", summary);
        return Result.success(result);
    }

    /**
     * 组装用户健康档案文本(身高体重/目标/活动量/健康目标/忌口/每日推荐指标)
     */
    private String buildHealthProfile(Long userId) {
        SysUser user = userService.getById(userId);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("性别%s、年龄%s岁、身高%scm、体重%skg、目标体重%s、活动量%s、健康目标%s、忌口%s",
                user.getGender() != null && user.getGender() == 1 ? "男" : "女",
                user.getAge() != null ? user.getAge() : "未填",
                user.getHeight() != null ? user.getHeight() : "未填",
                user.getWeight() != null ? user.getWeight() : "未填",
                user.getTargetWeight() != null ? user.getTargetWeight() + "kg" : "未设置",
                activityText(user.getActivityLevel()),
                goalText(user.getHealthGoal()),
                user.getAllergy() == null || user.getAllergy().isBlank() ? "无" : user.getAllergy()));
        try {
            HealthCalcVO calc = healthCalcService.getProfile(userId);
            sb.append(String.format("；每日推荐%skcal、蛋白质目标%s g、碳水目标%s g、脂肪目标%s g",
                    calc.getDailyCalorie(), calc.getProteinGram(), calc.getCarbGram(), calc.getFatGram()));
        } catch (BusinessException ignored) {
            // 档案未完善时跳过推荐值
        }
        return sb.toString();
    }

    /**
     * 组装今日实际摄入文本
     */
    private String buildDailyIntake(DailySummaryVO s) {
        return String.format("总热量%s kcal、蛋白质%s g、碳水%s g、脂肪%s g、钠%s mg、膳食纤维%s g",
                nvl(s.getTotalCalorie()), nvl(s.getTotalProtein()), nvl(s.getTotalCarbohydrate()),
                nvl(s.getTotalFat()), nvl(s.getTotalSodium()), nvl(s.getTotalFiber()));
    }

    /**
     * 组装数据对比结论文本(供大模型理解差异)
     */
    private String buildComparison(DailySummaryVO s) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("热量: 实际%s kcal vs 推荐%s kcal, 差值%s kcal(正为超标)",
                nvl(s.getTotalCalorie()), nvl(s.getRecommendCalorie()), signedDiff(s.getCalorieDiff())));
        if (s.getProteinDiff() != null) {
            sb.append(String.format("；蛋白质差值%s g", signedDiff(s.getProteinDiff())));
        }
        if (s.getCarbDiff() != null) {
            sb.append(String.format("；碳水差值%s g", signedDiff(s.getCarbDiff())));
        }
        if (s.getFatDiff() != null) {
            sb.append(String.format("；脂肪差值%s g", signedDiff(s.getFatDiff())));
        }
        DailySummaryVO.MealSummary dinner = s.getMeals().stream()
                .filter(m -> "DINNER".equals(m.getMealType())).findFirst().orElse(null);
        if (dinner != null && dinner.getTargetCalorie() != null) {
            sb.append(String.format("；晚餐实际%s kcal vs 目标%s kcal",
                    nvl(dinner.getCalorie()), nvl(dinner.getTargetCalorie())));
        }
        return sb.toString();
    }

    /**
     * 本地规则引擎生成建议(ai.mock=true 或未配置Key时联调用, 与真实模式输出口径一致)
     */
    private String buildRuleAdvice(DailySummaryVO summary) {
        StringBuilder advice = new StringBuilder();
        // 热量对比
        BigDecimal diff = summary.getCalorieDiff();
        advice.append(String.format("今日已摄入%s kcal（推荐%s kcal）。",
                summary.getTotalCalorie(), summary.getRecommendCalorie()));
        if (diff == null || diff.abs().compareTo(new BigDecimal("100")) <= 0) {
            advice.append("热量控制非常精准，请继续保持！");
        } else if (diff.signum() > 0) {
            advice.append(String.format("已超标%s kcal，", diff.setScale(0, RoundingMode.HALF_UP)));
            if (diff.compareTo(new BigDecimal("400")) > 0) {
                advice.append("建议晚餐以清淡蔬菜为主，减少主食，餐后散步30分钟。");
            } else {
                advice.append("建议晚餐适当减少主食份量，避免含糖饮料与零食。");
            }
        } else {
            advice.append(String.format("尚有%s kcal余量，", diff.abs().setScale(0, RoundingMode.HALF_UP)));
            advice.append("可补充一份蛋白质(如鸡蛋、牛奶)或蔬果，避免过度节食。");
        }
        // 蛋白对比
        if (summary.getProteinDiff() != null && summary.getProteinDiff().signum() < 0) {
            advice.append(String.format("蛋白质距目标还差%s g，可加一杯牛奶或一个鸡蛋。",
                    summary.getProteinDiff().abs().setScale(0, RoundingMode.HALF_UP)));
        }
        // 分餐结构提示
        DailySummaryVO.MealSummary dinner = summary.getMeals().stream()
                .filter(m -> "DINNER".equals(m.getMealType())).findFirst().orElse(null);
        if (dinner != null && dinner.getTargetCalorie() != null
                && dinner.getCalorie().compareTo(dinner.getTargetCalorie()
                        .multiply(new BigDecimal("1.3"))) > 0) {
            advice.append("晚餐摄入偏多，注意三餐热量结构(早30%/午40%/晚30%)。");
        }
        advice.append("仅供饮食参考，不构成医疗建议。");
        return advice.toString();
    }

    /** 加载Prompt模板(带缓存) */
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

    /** null安全的整数取整输出 */
    private String nvl(BigDecimal v) {
        return v == null ? "0" : v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    /** 差值前缀正负号输出 */
    private String signedDiff(BigDecimal v) {
        if (v == null) {
            return "0";
        }
        String s = v.abs().setScale(0, RoundingMode.HALF_UP).toPlainString();
        return v.signum() > 0 ? "+" + s : "-" + s;
    }

    /** 活动量文本 */
    private String activityText(String level) {
        if (level == null) {
            return "未填";
        }
        return switch (level) {
            case "SEDENTARY" -> "久坐";
            case "MODERATE" -> "中度活动";
            case "HIGH" -> "高度活动";
            default -> "轻度活动";
        };
    }

    /** 健康目标文本 */
    private String goalText(String goal) {
        if (goal == null) {
            return "维持体重";
        }
        return switch (goal) {
            case "LOSE" -> "减脂";
            case "GAIN" -> "增重";
            default -> "维持体重";
        };
    }
}