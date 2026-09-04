package com.diet.service;

import com.diet.dto.DailySummaryVO;
import com.diet.dto.HealthCalcVO;
import com.diet.dto.RiskReportVO;
import com.diet.entity.SysUser;

import java.time.LocalDate;

/**
 * 健康计算服务接口
 * 核心公式: Mifflin-St Jeor
 * 男 BMR = 10×体重 + 6.25×身高 - 5×年龄 + 5
 * 女 BMR = 10×体重 + 6.25×身高 - 5×年龄 - 161
 *
 * @author diet
 */
public interface HealthCalcService {

    /**
     * 按用户档案计算健康指标(BMR/TDEE/推荐热量/三大营养素/三餐分配)
     *
     * @param user 用户实体(含身高体重年龄性别活动量目标)
     * @return 计算结果
     */
    HealthCalcVO calc(SysUser user);

    /**
     * 计算并保存健康档案(写入health_profile表)
     *
     * @param userId 用户ID
     * @return 计算结果
     */
    HealthCalcVO calcAndSave(Long userId);

    /**
     * 更新自定义三餐比例并返回最新计算结果
     *
     * @param userId    用户ID
     * @param mealRatio 比例串(如"30,40,30")
     * @return 更新后的计算结果
     */
    HealthCalcVO updateMealRatio(Long userId, String mealRatio);

    /**
     * 查询用户档案(无档案时自动按用户信息计算
     *
     * @param userId 用户ID
     * @return 计算结果
     */
    HealthCalcVO getProfile(Long userId);

    /**
     * 每日摄入汇总: 实际摄入 vs 推荐值, 含分餐明细
     *
     * @param userId 用户ID
     * @param date   汇总日期
     * @return 汇总结果
     */
    DailySummaryVO dailySummary(Long userId, LocalDate date);

    /**
     * 阶段性风险分析(周/月): 热量失衡/蛋白不足/高盐/高糖/纤维不足等
     *
     * @param userId 用户ID
     * @param days   分析周期天数(7-周, 30-月)
     * @return 风险报告
     */
    RiskReportVO riskAnalysis(Long userId, int days);
}