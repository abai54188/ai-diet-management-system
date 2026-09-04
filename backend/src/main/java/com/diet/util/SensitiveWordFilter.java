package com.diet.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 敏感词过滤器
 * 发布动态/评论时调用, 命中词以***替换
 * 词库配置于 application.yml 的 sensitive.words
 *
 * @author diet
 */
@Component
public class SensitiveWordFilter {

    /** 敏感词库 */
    private final List<String> words;

    public SensitiveWordFilter(@Value("${sensitive.words:}") String wordsConfig) {
        // 逗号分隔配置解析为词库
        this.words = (wordsConfig == null || wordsConfig.isBlank()) ? List.of()
                : java.util.Arrays.stream(wordsConfig.split("[,，]"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /**
     * 过滤文本: 命中敏感词以***替换
     *
     * @param text 原文本
     * @return 过滤后文本
     */
    public String filter(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String result = text;
        for (String word : words) {
            if (StringUtils.hasText(word)) {
                result = result.replace(word, "***");
            }
        }
        return result;
    }

    /**
     * 是否包含敏感词
     */
    public boolean contains(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return words.stream().anyMatch(text::contains);
    }
}