package com.diet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.WeightRecord;
import com.diet.mapper.WeightRecordMapper;
import com.diet.service.WeightRecordService;
import org.springframework.stereotype.Service;

/**
 * 体重记录 服务实现类
 *
 * @author diet
 */
@Service
public class WeightRecordServiceImpl extends ServiceImpl<WeightRecordMapper, WeightRecord> implements WeightRecordService {
}