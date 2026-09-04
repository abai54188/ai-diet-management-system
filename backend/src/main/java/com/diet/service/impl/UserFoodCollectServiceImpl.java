package com.diet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.UserFoodCollect;
import com.diet.mapper.UserFoodCollectMapper;
import com.diet.service.UserFoodCollectService;
import org.springframework.stereotype.Service;

/**
 * 用户收藏食材 服务实现类
 *
 * @author diet
 */
@Service
public class UserFoodCollectServiceImpl extends ServiceImpl<UserFoodCollectMapper, UserFoodCollect> implements UserFoodCollectService {
}