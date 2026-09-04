package com.diet.controller;

import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.dto.WeightTrendVO;
import com.diet.service.WeightTrendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 体重趋势控制器
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/weight")
public class WeightTrendController {

    private final WeightTrendService weightTrendService;

    public WeightTrendController(WeightTrendService weightTrendService) {
        this.weightTrendService = weightTrendService;
    }

    /**
     * 体重趋势与达标预测(变化曲线数据+热量缺口+预计达标时间)
     *
     * @param days 统计周期天数, 默认30
     */
    @GetMapping("/trend")
    public Result<WeightTrendVO> trend(@RequestParam(required = false) Integer days) {
        Long userId = UserContext.get().getUserId();
        return Result.success(weightTrendService.trend(userId, days));
    }
}