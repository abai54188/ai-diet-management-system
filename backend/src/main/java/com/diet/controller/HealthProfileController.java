package com.diet.controller;

import com.diet.common.BusinessException;
import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.dto.DailySummaryVO;
import com.diet.dto.HealthCalcVO;
import com.diet.dto.MealRatioDTO;
import com.diet.dto.RiskReportVO;
import com.diet.service.HealthCalcService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 健康档案控制器
 * Mifflin-St Jeor公式计算每日推荐热量与三大营养素配比
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/health")
public class HealthProfileController {

    private final HealthCalcService healthCalcService;

    public HealthProfileController(HealthCalcService healthCalcService) {
        this.healthCalcService = healthCalcService;
    }

    /**
     * 计算并保存健康档案(BMR/TDEE/推荐热量/宏量配比)
     */
    @PostMapping("/calc")
    public Result<HealthCalcVO> calcAndSave() {
        Long userId = UserContext.get().getUserId();
        return Result.success(healthCalcService.calcAndSave(userId));
    }

    /**
     * 查询当前档案计算结果(无档案时按用户信息实时计算)
     */
    @GetMapping("/profile")
    public Result<HealthCalcVO> profile() {
        Long userId = UserContext.get().getUserId();
        return Result.success(healthCalcService.getProfile(userId));
    }

    /**
     * 自定义三餐热量比例(默认30/40/30, 三项之和须为100)
     */
    @PostMapping("/meal-ratio")
    public Result<HealthCalcVO> setMealRatio(@Validated @RequestBody MealRatioDTO dto) {
        Long userId = UserContext.get().getUserId();
        // 校验比例之和为100
        BigDecimal sum = dto.getBreakfast().add(dto.getLunch()).add(dto.getDinner());
        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new BusinessException("三餐比例之和必须等于100%");
        }
        String mealRatio = dto.getBreakfast().intValue() + "," + dto.getLunch().intValue() + ","
                + dto.getDinner().intValue();
        return Result.success(healthCalcService.updateMealRatio(userId, mealRatio));
    }

    /**
     * 每日摄入汇总(实际 vs 推荐, 含分餐明细)
     *
     * @param date 日期(yyyy-MM-dd), 默认今天
     */
    @GetMapping("/daily-summary")
    public Result<DailySummaryVO> dailySummary(@RequestParam(required = false) String date) {
        Long userId = UserContext.get().getUserId();
        LocalDate d = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        return Result.success(healthCalcService.dailySummary(userId, d));
    }

    /**
     * 阶段性风险分析
     *
     * @param days 周期天数, 默认7(周), 可传30(月)
     */
    @GetMapping("/risk")
    public Result<RiskReportVO> riskAnalysis(@RequestParam(defaultValue = "7") Integer days) {
        Long userId = UserContext.get().getUserId();
        return Result.success(healthCalcService.riskAnalysis(userId, days));
    }
}