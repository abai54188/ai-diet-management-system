package com.diet.util;

import com.diet.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 通用大模型调用客户端(兼容OpenAI接口格式)
 * 地址/密钥/模型均可在 application.yml 的 ai 配置块中修改
 *
 * @author diet
 */
@Component
public class AiClient {

    /** OpenAI兼容服务地址 */
    private final RestClient restClient;

    /** API密钥 */
    private final String apiKey;

    /** 模型名称 */
    private final String model;

    public AiClient(@Value("${ai.base-url}") String baseUrl,
                    @Value("${ai.api-key}") String apiKey,
                    @Value("${ai.model}") String model,
                    @Value("${ai.timeout-ms:60000}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * 密钥是否已配置(未配置时上层服务走本地模拟逻辑或直接报错)
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"sk-xxx".equals(apiKey.trim());
    }

    /**
     * 对话接口: 传入系统提示词与用户提示词，返回模型文本回复
     *
     * @param systemPrompt 系统提示词(角色与输出约束)
     * @param userPrompt   用户提示词(具体任务内容)
     * @return 模型回复文本
     */
    public String chat(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            throw new BusinessException("AI服务未配置api-key，请在application.yml中修改ai.api-key");
        }
        // 构造OpenAI chat/completions请求体
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        try {
            JsonNode resp = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return resp.path("choices").path(0).path("message").path("content").asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("AI服务调用失败: " + e.getMessage());
        }
    }
}