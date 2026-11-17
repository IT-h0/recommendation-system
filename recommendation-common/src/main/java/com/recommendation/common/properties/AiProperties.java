package com.recommendation.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.llm")
public class AiProperties {

    private String provider = "deepseek";
    private String baseUrl = "https://api.deepseek.com/v1";
    private String apiKey;
    private String model = "deepseek-flash";
    private Integer maxTokens = 2048;
    private Double temperature = 0.7;
    private Integer timeout = 60;
}
