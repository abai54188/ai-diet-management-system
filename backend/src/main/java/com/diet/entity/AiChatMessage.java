package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI营养问答历史消息实体类(对应 ai_chat_message 表)
 *
 * @author diet
 */
@Data
@TableName("ai_chat_message")
public class AiChatMessage {

    /** 消息ID(主键自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 角色: USER-用户提问, ASSISTANT-AI回答 */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createTime;
}