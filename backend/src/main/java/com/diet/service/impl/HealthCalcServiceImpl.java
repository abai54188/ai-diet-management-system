package com.diet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.common.BusinessException;
import com.diet.dto.DailySummaryVO;
import com.diet.dto.HealthCalcVO;
import com.diet.dto.RiskReportVO;
import com.diet.entity.DietRecord;
import com.diet.entity.FoodNutrition;
import com.diet.entity.HealthProfile;
import com.diet.entity.SysUser;
import com.diet.mapper.DietRecordMapper;
import com.diet.mapper.FoodNutritionMapper;
import com.diet.mapper.HealthProfileMapper;
import com.diet.service.HealthCalcService;
import com.diet.service.UserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 健康计算服务实现类
 *
 * 营养学标准依据:
 * 1. Mifflin-St Jeor公式(1990): 目前公认的BMR最优预测公式
 * 2. WHO活动系数: 久坐1.2 / 轻度1.375 / 中度1.55 / 高度1.725
 * 3. 安全热量调整: 减脂-400kcal/天(约0.5kg/周) / 增重+300kcal/天
 * 4. 供能比: 蛋白质/碳水4kcal每g, 脂肪9kcal每g
 * 5. 中国居民膳食指南: 钠<2000mg/天(约5g盐), 膳食纤维25-30g/天
 *
 * @author diet
 */
@Service
public class HealthCalcServiceImpl implements HealthCalcService {

    /** 健康提示免责声明(硬性约束: 所有健康提示末尾必须添加) */
    public static final String DISCLAIMER = "仅供饮食参考，不构成医疗建议。";

    /** 用餐时段中文标签 */
    private static final String[] MEAL_TYPES = {"BREAKFAST", "LUNCH", "DINNER", "SNACK"};
    private static final String[] MEAL_LABELS = {"早餐", "午餐", "晚餐", "加餐"};

    private final UserService userService;
    private final HealthProfileMapper profileMapper;
    private final DietRecordMapper dietRecordMapper;
    private final FoodNutritionMapper foodMapper;

    public HealthCalcServiceImpl(UserService userService, HealthProfileMapper profileMapper,
                                 DietRecordMapper dietRecordMapper, FoodNutritionMapper foodMapper) {
        this.userService = userService;
        this.profileMapper = profileMapper;
        this.dietRecordMapper = dietRecordMapper;
        this.foodMapper = foodMapper;
    }

    /**
     * Mifflin-St Jeor公式计算
     */
    @Override
    public HealthCalcVO calc(SysUser user) {
        if (user.getHeight() == null || user.getWeight() == null || user.getAge() == null || user.getGender() == null) {
            throw new BusinessException("请先完善身高、体重、年龄、性别等健康信息");
        }
        if (user.getGender() != 1 && user.getGender() != 2) {
            throw new BusinessException("性别数据不合法");
        }
        HealthCalcVO vo = new HealthCalcVO();
        vo.setUserId(user.getId());
        vo.setHeight(user.getHeight());
        vo.setWeight(user.getWeight());
        vo.setTargetWeight(user.getTargetWeight());
        vo.setAge(user.getAge());
        vo.setGender(user.getGender());
        vo.setActivityLevel(user.getActivityLevel());
        vo.setHealthGoal(user.getHealthGoal());

        // 1. BMR = 10×W + 6.25×H - 5×A + 性别项(男+5/女-161)
        BigDecimal genderTerm = user.getGender() == 1 ? new BigDecimal("5") : new BigDecimal("-161");
        BigDecimal bmr = user.getWeight().multiply(BigDecimal.TEN)
                .add(user.getHeight().multiply(new BigDecimal("6.25")))
                .subtract(BigDecimal.valueOf(user.getAge()).multiply(BigDecimal.valueOf(5)))
                .add(genderTerm);
        vo.setBmr(bmr.setScale(0, RoundingMode.HALF_UP));

        // 2. TDEE = BMR × 活动系数(WHO标准)
        BigDecimal factor = activityFactor(user.getActivityLevel());
        vo.setActivityFactor(factor);
        BigDecimal tdee = bmr.multiply(factor);
        vo.setTdee(tdee.setScale(0, RoundingMode.HALF_UP));

        // 3. 按健康目标调整推荐摄入: 减脂-400 / 增重+300 / 维持=TDEE
        String goal = user.getHealthGoal() == null ? "KEEP" : user.getHealthGoal();
        BigDecimal daily = tdee;
        if ("LOSE".equals(goal)) {
            daily = tdee.subtract(new BigDecimal("400")).max(new BigDecimal("1200"));
        } else if ("GAIN".equals(goal)) {
            daily = tdee.add(new BigDecimal("300"));
        }
        vo.setDailyCalorie(daily.setScale(0, RoundingMode.HALF_UP));

        // 4. 三大营养素供能比(按目标差异化): 减脂P25/C45/F30, 增重P25/C50/F25, 维持P20/C55/F25
        BigDecimal pRatio;
        BigDecimal cRatio;
        BigDecimal fRatio;
        switch (goal) {
            case "LOSE" -> {
                pRatio = new BigDecimal("25");
                cRatio = new BigDecimal("45");
                fRatio = new BigDecimal("30");
            }
            case "GAIN" -> {
                pRatio = new BigDecimal("25");
                cRatio = new BigDecimal("50");
                fRatio = new BigDecimal("25");
            }
            default -> {
                pRatio = new BigDecimal("20");
                cRatio = new BigDecimal("55");
                fRatio = new BigDecimal("25");
            }
        }
        vo.setProteinRatio(pRatio);
        vo.setCarbRatio(cRatio);
        vo.setFatRatio(fRatio);
        // 克数换算: 蛋白/碳水 4kcal每g, 脂肪 9kcal每g
        vo.setProteinGram(daily.multiply(pRatio).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP)
                .divide(new BigDecimal("4"), 0, RoundingMode.HALF_UP));
        vo.setCarbGram(daily.multiply(cRatio).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP)
                .divide(new BigDecimal("4"), 0, RoundingMode.HALF_UP));
        vo.setFatGram(daily.multiply(fRatio).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP)
                .divide(new BigDecimal("9"), 0, RoundingMode.HALF_UP));

        // 5. 三餐比例(已配置档案则读取自定义值)
        HealthProfile exist = profileMapper.selectOne(new LambdaQueryWrapper<HealthProfile>()
                .eq(HealthProfile::getUserId, user.getId()));
        vo.setMealRatio(exist != null && exist.getMealRatio() != null ? exist.getMealRatio() : "30,40,30");
        return vo;
    }

    /**
     * 计算并保存档案
     */
    @Override
    public HealthCalcVO calcAndSave(Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        HealthCalcVO vo = calc(user);
        // 写入health_profile表(存在则更新)
        HealthProfile exist = profileMapper.selectOne(new LambdaQueryWrapper<HealthProfile>()
                .eq(HealthProfile::getUserId, userId));
        if (exist == null) {
            exist = new HealthProfile();
            exist.setUserId(userId);
        }
        exist.setDailyCalorie(vo.getDailyCalorie());
        exist.setProteinRatio(vo.getProteinRatio());
        exist.setCarbRatio(vo.getCarbRatio());
        exist.setFatRatio(vo.getFatRatio());
        exist.setMealRatio(vo.getMealRatio());
        if (exist.getId() == null) {
            profileMapper.insert(exist);
        } else {
            profileMapper.updateById(exist);
        }
        return vo;
    }

    /**
     * 更新自定义三餐比例并返回最新计算结果
     */
    @Override
    public HealthCalcVO updateMealRatio(Long userId, String mealRatio) {
        // 先确保档案存在(触发一次完整计算)
        calcAndSave(userId);
        HealthProfile profile = profileMapper.selectOne(new LambdaQueryWrapper<HealthProfile>()
                .eq(HealthProfile::getUserId, userId));
        profile.setMealRatio(mealRatio);
        profileMapper.updateById(profile);
        HealthCalcVO vo = getProfile(userId);
        vo.setMealRatio(mealRatio);
        return vo;
    }

    /**
     * 查询档案(无则实时计算)
     */
    @Override
    public HealthCalcVO getProfile(Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return calc(user);
    }

    /**
     * 每日摄入汇总: 关联food_nutrition计算蛋白/碳水/脂肪/钠/纤维
     */
    @Override
    public DailySummaryVO dailySummary(Long userId, LocalDate date) {
        List<DietRecord> records = dietRecordMapper.selectList(new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, userId)
                .eq(DietRecord::getRecordDate, date));
        DailySummaryVO vo = new DailySummaryVO();
        vo.setDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE));

        // 推荐值(无档案信息时推荐值字段为空)
        HealthCalcVO calc;
        try {
            calc = getProfile(userId);
            vo.setRecommendCalorie(calc.getDailyCalorie());
        } catch (BusinessException e) {
            calc = null;
        }

        // 批量查询食材营养
        Map<Long, FoodNutrition> foodMap = Map.of();
        if (!records.isEmpty()) {
            foodMap = foodMapper.selectBatchIds(records.stream().map(DietRecord::getFoodId).toList())
                    .stream().collect(Collectors.toMap(FoodNutrition::getId, Function.identity()));
        }
        BigDecimal calorie = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carb = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal sodium = BigDecimal.ZERO;
        BigDecimal fiber = BigDecimal.ZERO;
        for (DietRecord r : records) {
            calorie = calorie.add(nvl(r.getCalorie()));
            FoodNutrition food = foodMap.get(r.getFoodId());
            if (food == null) {
                // AI整菜打卡(food_id为空): 直接累加已落库的宏量营养
                protein = protein.add(nvl(r.getProtein()));
                carb = carb.add(nvl(r.getCarbohydrate()));
                fat = fat.add(nvl(r.getFat()));
                continue;
            }
            // 换算系数 = 重量/100
            BigDecimal f = r.getWeight().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
            protein = protein.add(nvl(food.getProtein()).multiply(f));
            carb = carb.add(nvl(food.getCarbohydrate()).multiply(f));
            fat = fat.add(nvl(food.getFat()).multiply(f));
            sodium = sodium.add(nvl(food.getSodium()).multiply(f));
            fiber = fiber.add(nvl(food.getDietaryFiber()).multiply(f));
        }
        vo.setTotalCalorie(calorie.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalProtein(protein.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalCarbohydrate(carb.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalFat(fat.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalSodium(sodium.setScale(0, RoundingMode.HALF_UP));
        vo.setTotalFiber(fiber.setScale(1, RoundingMode.HALF_UP));

        // 与推荐值差值
        if (calc != null) {
            vo.setCalorieDiff(vo.getTotalCalorie().subtract(calc.getDailyCalorie()));
            vo.setProteinDiff(vo.getTotalProtein().subtract(new BigDecimal(calc.getProteinGram().toString())));
            vo.setCarbDiff(vo.getTotalCarbohydrate().subtract(new BigDecimal(calc.getCarbGram().toString())));
            vo.setFatDiff(vo.getTotalFat().subtract(new BigDecimal(calc.getFatGram().toString())));
        }

        // 分餐明细(三餐按比例分配目标热量, 加餐无目标)
        List<DailySummaryVO.MealSummary> meals = new ArrayList<>();
        String ratioStr = calc != null ? calc.getMealRatio() : "30,40,30";
        String[] ratios = ratioStr.split(",");
        for (int i = 0; i < MEAL_TYPES.length; i++) {
            DailySummaryVO.MealSummary meal = new DailySummaryVO.MealSummary();
            meal.setMealType(MEAL_TYPES[i]);
            meal.setMealLabel(MEAL_LABELS[i]);
            String mealType = MEAL_TYPES[i];
            List<DietRecord> mealRecords = records.stream()
                    .filter(r -> mealType.equals(r.getMealType())).toList();
            meal.setRecordCount(mealRecords.size());
            meal.setCalorie(mealRecords.stream().map(r -> nvl(r.getCalorie()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(1, RoundingMode.HALF_UP));
            if (calc != null && i < 3) {
                // 目标 = 每日推荐 × 餐次比例
                meal.setTargetCalorie(calc.getDailyCalorie()
                        .multiply(new BigDecimal(ratios[i])).divide(new BigDecimal("100"), 1, RoundingMode.HALF_UP));
            }
            meals.add(meal);
        }
        vo.setMeals(meals);
        return vo;
    }

    /**
     * 阶段性风险分析(规则引擎, 标准见类注释)
     */
    @Override
    public RiskReportVO riskAnalysis(Long userId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        HealthCalcVO calc;
        try {
            calc = getProfile(userId);
        } catch (BusinessException e) {
            throw new BusinessException("请先完善健康档案(身高/体重/年龄/性别)后再进行分析");
        }

        // 逐日汇总再取平均
        List<DietRecord> records = dietRecordMapper.selectList(new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, userId)
                .between(DietRecord::getRecordDate, start, end));
        Map<LocalDate, List<DietRecord>> byDate = records.stream()
                .collect(Collectors.groupingBy(DietRecord::getRecordDate));
        int recordDays = byDate.size();
        if (recordDays == 0) {
            throw new BusinessException("该时段暂无饮食记录，请先打卡几天再来分析");
        }
        Map<Long, FoodNutrition> foodMap = records.isEmpty() ? Map.of()
                : foodMapper.selectBatchIds(records.stream().map(DietRecord::getFoodId).distinct().toList())
                .stream().collect(Collectors.toMap(FoodNutrition::getId, Function.identity()));

        BigDecimal calorie = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carb = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal sodium = BigDecimal.ZERO;
        BigDecimal fiber = BigDecimal.ZERO;
        for (DietRecord r : records) {
            calorie = calorie.add(nvl(r.getCalorie()));
            FoodNutrition food = foodMap.get(r.getFoodId());
            if (food == null) {
                // AI整菜打卡(food_id为空): 直接累加已落库的宏量营养
                protein = protein.add(nvl(r.getProtein()));
                carb = carb.add(nvl(r.getCarbohydrate()));
                fat = fat.add(nvl(r.getFat()));
                continue;
            }
            BigDecimal f = r.getWeight().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
            protein = protein.add(nvl(food.getProtein()).multiply(f));
            carb = carb.add(nvl(food.getCarbohydrate()).multiply(f));
            fat = fat.add(nvl(food.getFat()).multiply(f));
            sodium = sodium.add(nvl(food.getSodium()).multiply(f));
            fiber = fiber.add(nvl(food.getDietaryFiber()).multiply(f));
        }
        BigDecimal d = BigDecimal.valueOf(recordDays);
        RiskReportVO vo = new RiskReportVO();
        vo.setPeriod(start + " ~ " + end);
        vo.setRecordDays(recordDays);
        vo.setAvgCalorie(calorie.divide(d, 1, RoundingMode.HALF_UP));
        vo.setAvgRecommendCalorie(calc.getDailyCalorie());
        vo.setAvgProtein(protein.divide(d, 1, RoundingMode.HALF_UP));
        vo.setAvgCarbohydrate(carb.divide(d, 1, RoundingMode.HALF_UP));
        vo.setAvgFat(fat.divide(d, 1, RoundingMode.HALF_UP));
        vo.setAvgSodium(sodium.divide(d, 0, RoundingMode.HALF_UP));
        vo.setAvgFiber(fiber.divide(d, 1, RoundingMode.HALF_UP));

        // 供能占比: (g×kcal系数 / 总热量)×100
        BigDecimal totalKcalFromMacro = protein.multiply(new BigDecimal("4"))
                .add(carb.multiply(new BigDecimal("4")))
                .add(fat.multiply(new BigDecimal("9")));
        if (totalKcalFromMacro.compareTo(BigDecimal.ZERO) > 0) {
            vo.setCarbEnergyRatio(carb.multiply(new BigDecimal("4"))
                    .multiply(BigDecimal.valueOf(100)).divide(totalKcalFromMacro, 1, RoundingMode.HALF_UP));
            vo.setFatEnergyRatio(fat.multiply(new BigDecimal("9"))
                    .multiply(BigDecimal.valueOf(100)).divide(totalKcalFromMacro, 1, RoundingMode.HALF_UP));
        } else {
            vo.setCarbEnergyRatio(BigDecimal.ZERO);
            vo.setFatEnergyRatio(BigDecimal.ZERO);
        }

        // ============ 风险项判定(等级: 1-提示 2-中风险 3-高风险) ============
        List<RiskReportVO.RiskItem> risks = new ArrayList<>();
        BigDecimal recCal = calc.getDailyCalorie();
        // 1. 热量失衡
        BigDecimal calDev = vo.getAvgCalorie().subtract(recCal).abs()
                .multiply(BigDecimal.valueOf(100)).divide(recCal, 1, RoundingMode.HALF_UP);
        if (calDev.compareTo(new BigDecimal("25")) >= 0) {
            risks.add(risk("CALORIE", "热量失衡", 3, vo.getAvgCalorie() + "kcal",
                    String.format("日均摄入%s，偏离推荐%s约%s%%", vo.getAvgCalorie(), recCal, calDev),
                    "LOSE".equals(user.getHealthGoal()) ? "严格记录饮食，主食定量，减少高油高糖零食"
                            : "按推荐热量安排三餐，主食与蛋白质均衡摄入"));
        } else if (calDev.compareTo(new BigDecimal("15")) >= 0) {
            risks.add(risk("CALORIE", "热量偏高/偏低", 2, vo.getAvgCalorie() + "kcal",
                    String.format("日均摄入偏离推荐约%s%%，长期可能影响体重管理目标", calDev),
                    "微调主食份量，保持热量在推荐值±10%以内"));
        }
        // 2. 蛋白质不足(<推荐80%提示, <60%中风险)
        BigDecimal proteinTarget = new BigDecimal(calc.getProteinGram().toString());
        if (proteinTarget.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pRate = vo.getAvgProtein().multiply(BigDecimal.valueOf(100))
                    .divide(proteinTarget, 1, RoundingMode.HALF_UP);
            if (pRate.compareTo(new BigDecimal("60")) < 0) {
                risks.add(risk("PROTEIN", "蛋白质不足", 2, vo.getAvgProtein() + "g",
                        String.format("日均蛋白%s g，仅达目标的%s%%，影响肌肉维持与饱腹感", vo.getAvgProtein(), pRate),
                        "每餐增加掌心大小的瘦肉/鱼虾/豆制品，早餐加鸡蛋或牛奶"));
            }
        }
        // 3. 高盐(钠: 中>2000mg, 高>3000mg)
        if (vo.getAvgSodium().compareTo(new BigDecimal("3000")) >= 0) {
            risks.add(risk("SODIUM", "高盐饮食", 3, vo.getAvgSodium() + "mg",
                    String.format("日均钠摄入%s mg，超过每日3000mg，血压风险高", vo.getAvgSodium()),
                    "减少外卖与酱料，用香辛料替代部分食盐，少吃腌制食品"));
        } else if (vo.getAvgSodium().compareTo(new BigDecimal("2000")) >= 0) {
            risks.add(risk("SODIUM", "钠摄入偏高", 2, vo.getAvgSodium() + "mg",
                    String.format("日均钠摄入%s mg，超过膳食指南建议的2000mg", vo.getAvgSodium()),
                    "烹饪食盐控制在5g/天内，减少咸菜、酱菜与加工肉制品"));
        }
        // 4. 高糖(碳水供能比>65%中风险)
        if (vo.getCarbEnergyRatio().compareTo(new BigDecimal("65")) >= 0) {
            risks.add(risk("SUGAR", "高糖/高碳水", 2, vo.getCarbEnergyRatio() + "%",
                    String.format("碳水供能占比%s%%，超过推荐的50-65%%上限", vo.getCarbEnergyRatio()),
                    "减少精米白面与含糖饮料，部分替换为杂粮与薯类"));
        }
        // 5. 膳食纤维不足(中<20g, 高不足<12g)
        if (vo.getAvgFiber().compareTo(new BigDecimal("12")) < 0) {
            risks.add(risk("FIBER", "膳食纤维严重不足", 3, vo.getAvgFiber() + "g",
                    String.format("日均纤维仅%s g，远低于25g建议量，影响肠道健康", vo.getAvgFiber()),
                    "每日蔬菜300-500g，主食掺入燕麦/糙米/豆类，水果适量"));
        } else if (vo.getAvgFiber().compareTo(new BigDecimal("20")) < 0) {
            risks.add(risk("FIBER", "膳食纤维不足", 2, vo.getAvgFiber() + "g",
                    String.format("日均纤维%s g，低于25g建议量", vo.getAvgFiber()),
                    "每餐搭配一份绿叶菜，加餐选苹果、燕麦片等高纤食物"));
        }
        // 6. 脂肪供能比过高(>35%)
        if (vo.getFatEnergyRatio().compareTo(new BigDecimal("35")) >= 0) {
            risks.add(risk("BALANCE", "脂肪供能过高", 2, vo.getFatEnergyRatio() + "%",
                    String.format("脂肪供能占比%s%%，超过20-30%%推荐区间", vo.getFatEnergyRatio()),
                    "减少油炸食品与肥肉，烹调油控制在25-30g/天"));
        }
        // 等级降序排序
        risks.sort((a, b) -> b.getLevel() - a.getLevel());
        vo.setRisks(risks);
        // 总体评语(拼接免责声明)
        String overall;
        if (risks.isEmpty()) {
            overall = String.format("近%d天饮食整体均衡，热量与营养素均符合推荐标准，请继续保持！", recordDays);
        } else {
            long high = risks.stream().filter(r -> r.getLevel() >= 3).count();
            long mid = risks.stream().filter(r -> r.getLevel() == 2).count();
            overall = String.format("近%d天检出高风险%d项、中风险%d项，建议优先改善%s。",
                    recordDays, high, mid, risks.stream().map(RiskReportVO.RiskItem::getName)
                            .limit(2).collect(Collectors.joining("、")));
        }
        vo.setOverallComment(overall + DISCLAIMER);
        return vo;
    }

    /** 构建风险项 */
    private RiskReportVO.RiskItem risk(String type, String name, int level, String value, String desc, String suggestion) {
        RiskReportVO.RiskItem item = new RiskReportVO.RiskItem();
        item.setType(type);
        item.setName(name);
        item.setLevel(level);
        item.setLevelText(level >= 3 ? "高风险" : "中风险");
        item.setDescription(desc + "。" + suggestion + "。" + DISCLAIMER);
        item.setSuggestion(suggestion);
        return item;
    }

    /** 活动系数(WHO标准) */
    private BigDecimal activityFactor(String level) {
        if (level == null) {
            return new BigDecimal("1.375");
        }
        return switch (level) {
            case "SEDENTARY" -> new BigDecimal("1.2");
            case "MODERATE" -> new BigDecimal("1.55");
            case "HIGH" -> new BigDecimal("1.725");
            default -> new BigDecimal("1.375");
        };
    }

    /** null安全 */
    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}