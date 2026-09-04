package com.diet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.diet.common.BusinessException;
import com.diet.dto.LoginDTO;
import com.diet.dto.LoginVO;
import com.diet.dto.RegisterDTO;
import com.diet.dto.UserProfileDTO;
import com.diet.dto.UserVO;
import com.diet.entity.SysUser;
import com.diet.mapper.UserMapper;
import com.diet.service.UserService;
import com.diet.util.JwtUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现类
 *
 * @author diet
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements UserService {

    /** BCrypt 密码加密器(无状态工具，直接持有实例) */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final JwtUtil jwtUtil;

    public UserServiceImpl(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void register(RegisterDTO dto) {
        // 校验用户名是否已存在
        SysUser exist = lambdaQuery().eq(SysUser::getUsername, dto.getUsername()).one();
        if (exist != null) {
            throw new BusinessException("用户名已存在，请更换后重试");
        }
        // 构建用户实体并保存，默认角色为普通用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setRole("USER");
        this.save(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 根据用户名查询用户
        SysUser user = lambdaQuery().eq(SysUser::getUsername, dto.getUsername()).one();
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        // 被禁用账号禁止登录
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        // 生成 JWT 令牌
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, toVO(user));
    }

    @Override
    public UserVO getUserById(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserVO updateProfile(Long userId, UserProfileDTO dto) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 仅更新传入的健康信息字段
        if (dto.getHeight() != null) {
            user.setHeight(dto.getHeight());
        }
        if (dto.getWeight() != null) {
            user.setWeight(dto.getWeight());
        }
        if (dto.getTargetWeight() != null) {
            user.setTargetWeight(dto.getTargetWeight());
        }
        if (dto.getAge() != null) {
            user.setAge(dto.getAge());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (StringUtils.hasText(dto.getActivityLevel())) {
            user.setActivityLevel(dto.getActivityLevel());
        }
        if (StringUtils.hasText(dto.getHealthGoal())) {
            user.setHealthGoal(dto.getHealthGoal());
        }
        if (dto.getAllergy() != null) {
            user.setAllergy(dto.getAllergy());
        }
        this.updateById(user);
        return toVO(user);
    }

    @Override
    public IPage<UserVO> pageUsers(Long current, Long size, String keyword) {
        // 构造分页与查询条件(用户名/昵称模糊匹配)
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getNickname, keyword))
                .orderByDesc(SysUser::getCreateTime);
        // 执行分页查询并转换为视图对象
        Page<SysUser> userPage = this.page(page, wrapper);
        Page<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 实体转视图对象(剔除密码等敏感字段)
     */
    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}