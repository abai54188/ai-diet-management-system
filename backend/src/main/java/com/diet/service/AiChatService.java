package com.diet.service;

import com.diet.dto.ChatMessageVO;
import com.diet.dto.ChatSendDTO;

import java.util.List;

/**
 * AI营养问答服务接口
 *
 * @author diet
 */
public interface AiChatService {

    /**
     * 发送问题(关联用户健康数据，保存历史)
     *
     * @param userId 用户ID
     * @param dto     问题
     * @return AI回答消息
     */
    ChatMessageVO send(Long userId, ChatSendDTO dto);

    /**
     * 查询历史对话
     *
     * @param userId 用户ID
     * @return 按时间正序的消息列表
     */
    List<ChatMessageVO> history(Long userId);
}