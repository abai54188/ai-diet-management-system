package com.diet.service;

import com.diet.dto.WeightTrendVO;

/**
 * 体重趋势服务接口
 *
 * @author diet
 */
public interface WeightTrendService {

    /**
     * 计算体重趋势与达标预测
     *
     * @param userId 用户ID
     * @param days   统计周期天数(默认30)
     * @return 趋势与预测结果
     */
    WeightTrendVO trend(Long userId, Integer days);
}