package com.diet.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.diet.common.Result;
import com.diet.dto.UserVO;
import com.diet.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器(仅管理员可访问，由 JwtInterceptor 统一校验)
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/admin/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 分页查询用户列表
     *
     * @param current 当前页码，默认1
     * @param size    每页条数，默认10
     * @param keyword 用户名/昵称关键字，可为空
     */
    @GetMapping("/page")
    public Result<IPage<UserVO>> page(@RequestParam(defaultValue = "1") Long current,
                                      @RequestParam(defaultValue = "10") Long size,
                                      @RequestParam(required = false) String keyword) {
        return Result.success(userService.pageUsers(current, size, keyword));
    }
}