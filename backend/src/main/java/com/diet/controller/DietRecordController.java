package com.diet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.entity.DietRecord;
import com.diet.service.DietRecordService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 饮食记录控制器
 * 记录归属当前登录用户，支持按日期区间、用餐时段筛选
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/diet")
public class DietRecordController {

    private final DietRecordService dietRecordService;

    private final com.diet.service.AiFoodService aiFoodService;

    public DietRecordController(DietRecordService dietRecordService, com.diet.service.AiFoodService aiFoodService) {
        this.dietRecordService = dietRecordService;
        this.aiFoodService = aiFoodService;
    }

    /**
     * AI解析食物营养: 输入食物/菜品名称，AI解析食材组成后由本地营养库计算营养
     * (AI仅输出食材名称+重量，营养数值全部来自本地库)
     */
    @org.springframework.web.bind.annotation.PostMapping("/ai-analyze")
    public Result<com.diet.dto.AiFoodParseVO> aiAnalyze(
            @org.springframework.validation.annotation.Validated @org.springframework.web.bind.annotation.RequestBody com.diet.dto.AiFoodParseDTO dto) {
        return Result.success(aiFoodService.parseFood(dto));
    }

    /**
     * 分页查询当前用户饮食记录
     *
     * @param current    当前页码
     * @param size       每页条数
     * @param mealType   用餐时段(BREAKFAST/LUNCH/DINNER/SNACK)
     * @param startDate  起始日期(含)
     * @param endDate    截止日期(含)
     */
    @GetMapping("/page")
    public Result<IPage<DietRecord>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String mealType,
                                          @RequestParam(required = false) LocalDate startDate,
                                          @RequestParam(required = false) LocalDate endDate) {
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, UserContext.get().getUserId())
                .eq(StringUtils.hasText(mealType), DietRecord::getMealType, mealType)
                .ge(startDate != null, DietRecord::getRecordDate, startDate)
                .le(endDate != null, DietRecord::getRecordDate, endDate)
                .orderByDesc(DietRecord::getRecordDate)
                .orderByDesc(DietRecord::getCreateTime);
        return Result.success(dietRecordService.page(new Page<>(current, size), wrapper));
    }

    /**
     * 新增饮食记录(自动绑定当前用户)
     */
    @PostMapping
    public Result<Void> save(@RequestBody DietRecord record) {
        record.setUserId(UserContext.get().getUserId());
        dietRecordService.save(record);
        return Result.success();
    }

    /**
     * 修改饮食记录(仅允许修改本人的记录)
     */
    @PutMapping
    public Result<Void> update(@RequestBody DietRecord record) {
        dietRecordService.lambdaUpdate()
                .eq(DietRecord::getId, record.getId())
                .eq(DietRecord::getUserId, UserContext.get().getUserId())
                .update(record);
        return Result.success();
    }

    /**
     * 删除饮食记录(仅允许删除本人的记录)
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dietRecordService.lambdaUpdate()
                .eq(DietRecord::getId, id)
                .eq(DietRecord::getUserId, UserContext.get().getUserId())
                .remove();
        return Result.success();
    }
}