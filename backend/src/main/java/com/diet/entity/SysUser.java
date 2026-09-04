package com.diet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体类(对应 sys_user 表)
 *
 * @author diet
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 用户ID(主键自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名(登录账号，唯一) */
    private String username;

    /** 密码(BCrypt加密存储) */
    private String password;

    /** 用户昵称 */
    private String nickname;

    /** 角色: USER-普通用户, ADMIN-管理员 */
    private String role;

    /** 状态: 0-正常, 1-禁用 */
    private Integer status;

    /** 身高(cm) */
    private BigDecimal height;

    /** 体重(kg) */
    private BigDecimal weight;

    /** 目标体重(kg) */
    private BigDecimal targetWeight;

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

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间(插入时自动填充) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间(插入/更新时自动填充) */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}