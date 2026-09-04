package com.diet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diet.entity.Recipe;

/**
 * 食谱表 Mapper 接口
 * 继承 BaseMapper 即拥有通用 CRUD 能力
 *
 * @author diet
 */
public interface RecipeMapper extends BaseMapper<Recipe> {
}