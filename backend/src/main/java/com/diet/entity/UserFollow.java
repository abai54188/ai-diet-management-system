package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户关注实体类(对应 user_follow 表)
 *
 * @author diet
 */
@Data
@TableName("user_follow")
public class UserFollow {

    /** 关注ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关注者ID */
    private Long followerId;

    /** 被关注者ID */
    private Long followeeId;

    /** 创建时间 */
    private LocalDateTime createTime;
}