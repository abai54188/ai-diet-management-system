package com.diet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI营养问答发送参数
 *
 * @author diet
 */
@Data
public class ChatSendDTO {

    /** 用户问题 */
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题长度不能超过500字")
    private String question;
}