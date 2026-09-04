package com.diet.controller;

import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.dto.LoginDTO;
import com.diet.dto.LoginVO;
import com.diet.dto.RegisterDTO;
import com.diet.dto.UserProfileDTO;
import com.diet.dto.UserVO;
import com.diet.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器(登录、注册、当前用户信息与健康资料)
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public Result<UserVO> info() {
        Long userId = UserContext.get().getUserId();
        return Result.success(userService.getUserById(userId));
    }

    /**
     * 更新当前用户健康信息(身高/体重/年龄/性别/活动量/健康目标/忌口)
     */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestBody UserProfileDTO dto) {
        Long userId = UserContext.get().getUserId();
        return Result.success(userService.updateProfile(userId, dto));
    }
}