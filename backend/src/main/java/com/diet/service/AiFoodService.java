package com.diet.service;

import com.diet.dto.AiFoodParseDTO;
import com.diet.dto.AiFoodParseVO;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 拍照识别食物: 视觉大模型识别图片中的食物/菜品并估算重量，按相同口径解析营养
     *
     * @param image 用户拍摄的照片(已由前端压缩)
     * @return 菜名、食材明细与营养汇总(与文本解析同结构)
     */
    AiFoodParseVO parseFoodPhoto(MultipartFile image);
}