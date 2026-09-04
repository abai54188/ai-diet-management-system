package com.diet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.DietRecord;
import com.diet.mapper.DietRecordMapper;
import com.diet.service.DietRecordService;
import org.springframework.stereotype.Service;

/**
 * 饮食记录 服务实现类
 *
 * @author diet
 */
@Service
public class DietRecordServiceImpl extends ServiceImpl<DietRecordMapper, DietRecord> implements DietRecordService {
}