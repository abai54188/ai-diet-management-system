package com.diet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.diet.dto.LoginDTO;
import com.diet.dto.LoginVO;
import com.diet.dto.RegisterDTO;
import com.diet.dto.UserProfileDTO;
import com.diet.dto.UserVO;
import com.diet.entity.SysUser;

/**
 * 用户服务接口
 *
 * @author diet
 */
public interface UserService extends IService<SysUser> {

    /**
     * 用户注册
     *
     * @param dto 注册参数
     */
    void register(RegisterDTO dto);

    /**
     * 用户登录
     *
     * @param dto 登录参数
     * @return 令牌与用户信息
     */
    LoginVO login(LoginDTO dto);

    /**
     * 查询指定用户信息
     *
     * @param userId 用户ID
     * @return 用户信息(不含密码)
     */
    UserVO getUserById(Long userId);

    /**
     * 更新当前用户健康信息(身高/体重/年龄/性别/活动量/健康目标/忌口)
     *
     * @param userId 用户ID
     * @param dto    健康信息参数
     * @return 更新后的用户信息
     */
    UserVO updateProfile(Long userId, UserProfileDTO dto);

    /**
     * 分页查询用户列表(管理员功能)
     *
     * @param current 当前页码
     * @param size    每页条数
     * @param keyword 用户名/昵称关键字
     * @return 分页结果
     */
    IPage<UserVO> pageUsers(Long current, Long size, String keyword);
}