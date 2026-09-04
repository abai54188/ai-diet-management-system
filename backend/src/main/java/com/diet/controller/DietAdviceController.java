package com.diet.controller;

import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.dto.DailySummaryVO;
import com.diet.service.HealthCalcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI饮食建议控制器
 * 每日对比实际摄入与推荐标准，生成自然语言调整建议(规则引擎, 数据全部来自本地计算)
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/health")
public class DietAdviceController {

    private final HealthCalcService healthCalcService;

    public DietAdviceController(HealthCalcService healthCalcService) {
        this.healthCalcService = healthCalcService;
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

        // 规则引擎生成建议内容(可无缝替换为AI生成: 组装数据调用AiClient)
        StringBuilder advice = new StringBuilder();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", summary.getDate());
        if (summary.getRecommendCalorie() == null) {
            advice.append("您还未完善健康档案，请先在健康档案页填写身高、体重等信息，获取个性化推荐摄入量。");
            result.put("advice", advice + "仅供饮食参考，不构成医疗建议。");
            return Result.success(result);
        }
        // 热量对比
        BigDecimal diff = summary.getCalorieDiff();
        advice.append(String.format("今日已摄入%s kcal（推荐%s kcal）。",
                summary.getTotalCalorie(), summary.getRecommendCalorie()));
        if (diff.abs().compareTo(new BigDecimal("100")) <= 0) {
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
        result.put("advice", advice.toString());
        result.put("summary", summary);
        return Result.success(result);
    }
}