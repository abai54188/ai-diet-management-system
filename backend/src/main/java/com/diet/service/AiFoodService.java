package com.diet.service;

import com.diet.dto.AiFoodParseDTO;
import com.diet.dto.AiFoodParseVO;

/**
 * AI食物营养解析服务接口
 *
 * @author diet
 */
public interface AiFoodService {

    /**
     * 解析食物/菜品: AI输出食材组成，本地营养库计算营养
     *
     * @param dto 食物名称与份数
     * @return 食材明细与营养汇总
     */
    AiFoodParseVO parseFood(AiFoodParseDTO dto);
}