package com.recommendation.server.ai.config;

import com.recommendation.common.properties.AiProperties;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

// 不用 langchain4j-spring-boot-starter（避免版本与 SB 3.2.0 冲突），
// 手动创建 OpenAiChatModel / OpenAiStreamingChatModel Bean
@Configuration
public class LangChain4jConfig {

    // application.yml 中 base-url 可能是 "https://api.deepseek.com"，不带 /v1 后缀；
    // LangChain4j 的 OpenAiChatModel 自动追加 "/chat/completions"，所以这里补齐 /v1
    private String ensureV1Suffix(String baseUrl) {
        if (baseUrl.endsWith("/v1")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + "v1";
        }
        return baseUrl + "/v1";
    }

    // 同步模型：用于工具调用循环（需要同步获取 tool-call 结果）和标题生成
    @Bean
    public OpenAiChatModel chatLanguageModel(AiProperties props) {
        return OpenAiChatModel.builder()
                .baseUrl(ensureV1Suffix(props.getBaseUrl()))
                .apiKey(props.getApiKey())
                .modelName(props.getModel())
                .temperature(props.getTemperature())
                .maxTokens(props.getMaxTokens())
                .timeout(Duration.ofSeconds(props.getTimeout()))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    // 流式模型：用于工具调用完成后的 SSE 文本输出，每 token 触发 onPartialResponse
    @Bean
    public OpenAiStreamingChatModel streamingChatLanguageModel(AiProperties props) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(ensureV1Suffix(props.getBaseUrl()))
                .apiKey(props.getApiKey())
                .modelName(props.getModel())
                .temperature(props.getTemperature())
                .maxTokens(props.getMaxTokens())
                .timeout(Duration.ofSeconds(props.getTimeout()))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
