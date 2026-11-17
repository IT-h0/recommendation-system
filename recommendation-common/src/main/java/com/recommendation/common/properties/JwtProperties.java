package com.recommendation.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 密钥
     */
    private String secret = "recommendation-system-jwt-secret-key-2024";

    /**
     * 过期时间(秒) 默认7天
     */
    private Long expiration = 604800L;

    /**
     * Token前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * Header名称
     */
    private String headerName = "Authorization";
}
