package com.diet.controller;

import com.diet.common.Result;
import com.diet.common.UserContext;
import com.diet.dto.ChatMessageVO;
import com.diet.dto.ChatSendDTO;
import com.diet.service.AiChatService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI营养问答控制器
 * 回答自动关联当前用户健康档案与近期摄入数据
 *
 * @author diet
 */
@RestController
@RequestMapping("/api/chat")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * 发送问题(保存一问一答历史)
     */
    @PostMapping("/send")
    public Result<ChatMessageVO> send(@Validated @RequestBody ChatSendDTO dto) {
        Long userId = UserContext.get().getUserId();
        return Result.success(aiChatService.send(userId, dto));
    }

    /**
     * 查询历史对话(按时间正序)
     */
    @GetMapping("/history")
    public Result<List<ChatMessageVO>> history() {
        Long userId = UserContext.get().getUserId();
        return Result.success(aiChatService.history(userId));
    }
}