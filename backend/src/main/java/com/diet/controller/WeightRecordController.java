package com.diet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.entity.WeightRecord;
import com.diet.service.WeightRecordService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 体重记录控制器
 * 记录归属当前登录用户，支持按日期区间筛选；同一天仅一条记录(重复记录按日期覆盖)
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/weight")
public class WeightRecordController {

    private final WeightRecordService weightRecordService;

    public WeightRecordController(WeightRecordService weightRecordService) {
        this.weightRecordService = weightRecordService;
    }

    /**
     * 分页查询当前用户体重记录(按日期倒序，支持日期区间)
     *
     * @param current   当前页码
     * @param size      每页条数
     * @param startDate 起始日期(含)
     * @param endDate   截止日期(含)
     */
    @GetMapping("/page")
    public Result<IPage<WeightRecord>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) LocalDate startDate,
                                            @RequestParam(required = false) LocalDate endDate) {
        LambdaQueryWrapper<WeightRecord> wrapper = new LambdaQueryWrapper<WeightRecord>()
                .eq(WeightRecord::getUserId, UserContext.get().getUserId())
                .ge(startDate != null, WeightRecord::getRecordDate, startDate)
                .le(endDate != null, WeightRecord::getRecordDate, endDate)
                .orderByDesc(WeightRecord::getRecordDate);
        return Result.success(weightRecordService.page(new Page<>(current, size), wrapper));
    }

    /**
     * 新增体重记录(同一天已有记录则覆盖更新)
     */
    @PostMapping
    public Result<Void> save(@RequestBody WeightRecord record) {
        Long userId = UserContext.get().getUserId();
        record.setUserId(userId);
        // 同一天重复记录 -> 更新为最新体重
        WeightRecord exist = weightRecordService.lambdaQuery()
                .eq(WeightRecord::getUserId, userId)
                .eq(WeightRecord::getRecordDate, record.getRecordDate())
                .one();
        if (exist != null) {
            exist.setWeight(record.getWeight());
            weightRecordService.updateById(exist);
        } else {
            record.setId(null);
            weightRecordService.save(record);
        }
        return Result.success();
    }

    /**
     * 删除体重记录(仅允许删除本人的记录)
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        weightRecordService.lambdaUpdate()
                .eq(WeightRecord::getId, id)
                .eq(WeightRecord::getUserId, UserContext.get().getUserId())
                .remove();
        return Result.success();
    }
}