package com.diet.util;

import com.diet.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
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

    /** AI服务地址(不含路径) */
    private final String endpoint;

    /** API密钥 */
    private final String apiKey;

    /** 模型名称 */
    private final String model;

    /** 请求读超时(毫秒) */
    private final int readTimeoutMs;

    public AiClient(@Value("${ai.base-url}") String baseUrl,
                    @Value("${ai.api-key}") String apiKey,
                    @Value("${ai.model}") String model,
                    @Value("${ai.timeout-ms:60000}") int timeoutMs) {
        this.endpoint = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.model = model;
        this.readTimeoutMs = timeoutMs;
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
        String url = endpoint + "/chat/completions";
        String json = writeJson(body);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(readTimeoutMs);
            // 明确声明JSON与UTF-8, 避免服务端按octet-stream处理
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            byte[] reqData = json.getBytes(StandardCharsets.UTF_8);
            // 显式指定内容长度并关闭分块, 规避Expect:100-continue等段被网络掐断
            conn.setFixedLengthStreamingMode(reqData.length);
            conn.getOutputStream().write(reqData);
            conn.getOutputStream().flush();

            int status = conn.getResponseCode();
            java.io.InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            byte[] respData = readAll(is);
            String text = new String(respData, StandardCharsets.UTF_8);
            if (status < 200 || status >= 300) {
                throw new BusinessException("AI服务返回错误(" + status + "): " + truncate(text, 300));
            }
            JsonNode node = new ObjectMapper().readTree(text);
            String content = node.path("choices").path(0).path("message").path("content").asText();
            if (content.isBlank()) {
                String tip = node.path("error").path("message").asText("");
                throw new BusinessException("AI服务未返回内容: " + (tip.isBlank() ? truncate(text, 200) : tip));
            }
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("AI服务调用失败(" + url + "): " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** 序列化请求体 */
    private String writeJson(Map<String, Object> body) {
        try {
            return new ObjectMapper().writeValueAsString(body);
        } catch (Exception e) {
            throw new BusinessException("AI请求体序列化失败");
        }
    }

    /** 读取流全部字节 */
    private byte[] readAll(java.io.InputStream is) throws java.io.IOException {
        if (is == null) {
            return new byte[0];
        }
        try (is; java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        }
    }

    /** 截断长文本便于错误提示 */
    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}