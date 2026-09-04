package com.diet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diet.entity.SysUser;

/**
 * 用户表 Mapper 接口
 * 继承 BaseMapper 即拥有通用 CRUD 能力
 *
 * @author diet
 */
public interface UserMapper extends BaseMapper<SysUser> {
}