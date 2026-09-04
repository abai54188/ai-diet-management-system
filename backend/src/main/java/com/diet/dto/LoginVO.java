package com.diet.dto;

import lombok.Data;

/**
 * 登录响应结果(令牌 + 用户信息)
 *
 * @author diet
 */
@Data
public class LoginVO {

    /** JWT 令牌 */
    private String token;

    /** 用户信息 */
    private UserVO userInfo;

    public LoginVO(String token, UserVO userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }
}