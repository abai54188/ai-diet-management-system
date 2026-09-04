package com.diet.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户信息视图对象(返回给前端，不含密码等敏感字段)
 *
 * @author diet
 */
@Data
public class UserVO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 角色: USER-普通用户, ADMIN-管理员 */
    private String role;

    /** 状态: 0-正常, 1-禁用 */
    private Integer status;

    /** 身高(cm) */
    private BigDecimal height;

    /** 体重(kg) */
    private BigDecimal weight;

    /** 年龄(岁) */
    private Integer age;

    /** 性别: 0-未知, 1-男, 2-女 */
    private Integer gender;

    /** 活动量: SEDENTARY-久坐, LIGHT-轻度, MODERATE-中度, HIGH-高度 */
    private String activityLevel;

    /** 健康目标: LOSE-减脂, KEEP-维持, GAIN-增重 */
    private String healthGoal;

    /** 忌口/过敏源(逗号分隔) */
    private String allergy;

    /** 创建时间 */
    private LocalDateTime createTime;
}