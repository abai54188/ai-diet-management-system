package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容举报实体类(对应 content_report 表)
 *
 * @author diet
 */
@Data
@TableName("content_report")
public class ContentReport {

    /** 举报ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人ID */
    private Long reporterId;

    /** 目标类型: POST-动态, COMMENT-评论 */
    private String targetType;

    /** 目标ID */
    private Long targetId;

    /** 举报理由 */
    private String reason;

    /** 状态: 0-待处理, 1-已处理 */
    private Integer status;

    /** 处理结果说明 */
    private String handleResult;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 处理时间 */
    private LocalDateTime handleTime;
}