package com.diet.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI问答消息视图对象
 *
 * @author diet
 */
@Data
public class ChatMessageVO {

    /** 消息ID */
    private Long id;

    /** 角色: USER-用户提问, ASSISTANT-AI回答 */
    private String role;

    /** 消息内容 */
    private String content;

    /** 发送时间 */
    private LocalDateTime createTime;
}