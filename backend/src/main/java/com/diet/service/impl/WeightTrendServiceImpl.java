package com.diet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.common.BusinessException;
import com.diet.dto.HealthCalcVO;
import com.diet.dto.WeightTrendVO;
import com.diet.entity.DietRecord;
import com.diet.entity.SysUser;
import com.diet.entity.WeightRecord;
import com.diet.mapper.DietRecordMapper;
import com.diet.mapper.WeightRecordMapper;
import com.diet.service.HealthCalcService;
import com.diet.service.UserService;
import com.diet.service.WeightTrendService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 体重趋势服务实现类
 * 核心原理: 7700kcal ≈ 1kg脂肪组织
 *
 * @author diet
 */
@Service
public class WeightTrendServiceImpl implements WeightTrendService {

    /** 1kg脂肪对应热量(kcal) */
    private static final BigDecimal KCAL_PER_KG = new BigDecimal("7700");

    private final WeightRecordMapper weightMapper;
    private final DietRecordMapper dietRecordMapper;
    private final UserService userService;
    private final HealthCalcService healthCalcService;

    public WeightTrendServiceImpl(WeightRecordMapper weightMapper, DietRecordMapper dietRecordMapper,
                                   UserService userService, HealthCalcService healthCalcService) {
        this.weightMapper = weightMapper;
        this.dietRecordMapper = dietRecordMapper;
        this.userService = userService;
        this.healthCalcService = healthCalcService;
    }

    /**
     * 计算体重趋势与达标预测
     * 优先用实际体重变化的线性斜率预测；记录不足时用饮食热量缺口推算
     */
    @Override
    public WeightTrendVO trend(Long userId, Integer days) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        int range = days == null || days < 7 ? 30 : Math.min(days, 180);
        LocalDate start = LocalDate.now().minusDays(range - 1L);
        List<WeightRecord> records = weightMapper.selectList(new LambdaQueryWrapper<WeightRecord>()
                .eq(WeightRecord::getUserId, userId)
                .ge(WeightRecord::getRecordDate, start)
                .orderByAsc(WeightRecord::getRecordDate));

        WeightTrendVO vo = new WeightTrendVO();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        vo.setRecords(records.stream().map(r -> {
            WeightTrendVO.WeightPoint p = new WeightTrendVO.WeightPoint();
            p.setDate(r.getRecordDate().format(fmt));
            p.setWeight(r.getWeight());
            return p;
        }).toList());
        vo.setTargetWeight(user.getTargetWeight());

        // 无记录场景
        if (records.isEmpty()) {
            vo.setCurrentWeight(user.getWeight());
            vo.setSuggestion("暂无体重记录，建议每天固定时间(晨起空腹)称重一次，连续记录后可查看趋势与达标预测。"
                    + HealthCalcServiceImpl.DISCLAIMER);
            return vo;
        }

        // 当前体重取最新记录
        WeightRecord latest = records.get(records.size() - 1);
        vo.setCurrentWeight(latest.getWeight());

        // 日均变化: 线性回归斜率(一元回归, x=距首日天数, y=体重)
        BigDecimal dailyChange = slope(records);
        vo.setDailyChange(dailyChange);
        if (user.getTargetWeight() == null) {
            vo.setSuggestion("建议设置目标体重，即可获得达标时间预测。" + HealthCalcServiceImpl.DISCLAIMER);
            return vo;
        }

        BigDecimal remaining = user.getTargetWeight().subtract(latest.getWeight());
        vo.setRemainingKg(remaining.setScale(1, RoundingMode.HALF_UP));
        // 已达标(误差0.2kg内)
        if (remaining.abs().compareTo(new BigDecimal("0.2")) <= 0) {
            vo.setForecastDays(0);
            vo.setForecastDate(LocalDate.now().format(fmt));
            vo.setSuggestion("已达到目标体重，建议切换为维持目标并保持当前饮食运动习惯。"
                    + HealthCalcServiceImpl.DISCLAIMER);
            return vo;
        }

        // 预测: 体重变化方向与目标一致时按斜率外推
        boolean directionOk = (remaining.compareTo(BigDecimal.ZERO) > 0 && dailyChange.compareTo(BigDecimal.ZERO) > 0)
                || (remaining.compareTo(BigDecimal.ZERO) < 0 && dailyChange.compareTo(BigDecimal.ZERO) < 0);
        if (directionOk && dailyChange.abs().compareTo(new BigDecimal("0.01")) >= 0) {
            int forecast = remaining.abs().divide(dailyChange.abs(), 0, RoundingMode.CEILING).intValue();
            vo.setForecastDays(forecast);
            vo.setForecastDate(LocalDate.now().plusDays(forecast).format(fmt));
            String pace = dailyChange.abs().compareTo(new BigDecimal("1.0")) > 0
                    ? "当前速度过快(日均>1kg)，建议放缓以保护肌肉与代谢。"
                    : "当前节奏健康(每周约" + dailyChange.abs().multiply(BigDecimal.valueOf(7))
                    .setScale(1, RoundingMode.HALF_UP) + "kg)。";
            vo.setSuggestion(String.format("按当前趋势%s天(预计%s)可达标。%s%s",
                    forecast, vo.getForecastDate(), pace, HealthCalcServiceImpl.DISCLAIMER));
            return vo;
        }

        // 无法按体重预测时, 用热量缺口推算: 需减/增kg × 7700 / 日均缺口
        BigDecimal gap = calorieGap(userId);
        vo.setCalorieGap(gap);
        if (gap != null && gap.abs().compareTo(new BigDecimal("100")) >= 0
                && gap.signum() == remaining.signum()) {
            int forecast = remaining.abs().multiply(KCAL_PER_KG)
                    .divide(gap.abs(), 0, RoundingMode.CEILING).intValue();
            vo.setForecastDays(forecast);
            vo.setForecastDate(LocalDate.now().plusDays(forecast).format(fmt));
            vo.setSuggestion(String.format("体重暂无明显趋势，按当前日均热量缺口%skcal推算，约%d天(预计%s)可达标。%s",
                    gap, forecast, vo.getForecastDate(), HealthCalcServiceImpl.DISCLAIMER));
        } else {
            vo.setSuggestion("体重变化趋势与目标方向不符或热量缺口不足，建议按推荐热量执行并增加运动消耗。"
                    + HealthCalcServiceImpl.DISCLAIMER);
        }
        return vo;
    }

    /**
     * 热量缺口 = 每日推荐摄入(已含目标调整) - 近7日实际日均摄入
     */
    private BigDecimal calorieGap(Long userId) {
        try {
            HealthCalcVO calc = healthCalcService.getProfile(userId);
            List<DietRecord> recents = dietRecordMapper.selectList(new LambdaQueryWrapper<DietRecord>()
                    .eq(DietRecord::getUserId, userId)
                    .ge(DietRecord::getRecordDate, LocalDate.now().minusDays(6)));
            if (recents.isEmpty()) {
                return null;
            }
            BigDecimal sum = recents.stream().map(r -> r.getCalorie() == null ? BigDecimal.ZERO : r.getCalorie())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = sum.divide(BigDecimal.valueOf(recents.size()), 1, RoundingMode.HALF_UP);
            return calc.getDailyCalorie().subtract(avg);
        } catch (BusinessException e) {
            return null;
        }
    }

    /** 线性回归斜率 */
    private BigDecimal slope(List<WeightRecord> records) {
        int n = records.size();
        if (n < 2) {
            return BigDecimal.ZERO;
        }
        BigDecimal sumX = BigDecimal.ZERO;
        BigDecimal sumY = BigDecimal.ZERO;
        BigDecimal sumXY = BigDecimal.ZERO;
        BigDecimal sumXX = BigDecimal.ZERO;
        LocalDate first = records.get(0).getRecordDate();
        for (WeightRecord r : records) {
            BigDecimal x = BigDecimal.valueOf(ChronoUnit.DAYS.between(first, r.getRecordDate()));
            BigDecimal y = r.getWeight();
            sumX = sumX.add(x);
            sumY = sumY.add(y);
            sumXY = sumXY.add(x.multiply(y));
            sumXX = sumXX.add(x.multiply(x));
        }
        BigDecimal denominator = BigDecimal.valueOf(n).multiply(sumXX).subtract(sumX.multiply(sumX));
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(n).multiply(sumXY).subtract(sumX.multiply(sumY))
                .divide(denominator, 4, RoundingMode.HALF_UP);
    }
}