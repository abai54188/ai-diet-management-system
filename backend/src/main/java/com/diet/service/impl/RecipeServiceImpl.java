package com.diet.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.entity.Recipe;
import com.diet.mapper.RecipeMapper;
import com.diet.service.RecipeService;
import org.springframework.stereotype.Service;

/**
 * 食谱 服务实现类
 *
 * @author diet
 */
@Service
public class RecipeServiceImpl extends ServiceImpl<RecipeMapper, Recipe> implements RecipeService {
}