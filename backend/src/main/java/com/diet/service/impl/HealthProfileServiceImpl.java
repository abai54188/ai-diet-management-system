package com.diet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.HealthProfile;
import com.diet.mapper.HealthProfileMapper;
import com.diet.service.HealthProfileService;
import org.springframework.stereotype.Service;

/**
 * 用户健康档案 服务实现类
 *
 * @author diet
 */
@Service
public class HealthProfileServiceImpl extends ServiceImpl<HealthProfileMapper, HealthProfile> implements HealthProfileService {
}