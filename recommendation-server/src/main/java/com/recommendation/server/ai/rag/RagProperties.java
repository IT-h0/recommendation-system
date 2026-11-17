package com.recommendation.server.ai.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 检索配置（Qdrant 向量库 + 百炼 embedding）。
 * 对应 application.yml 的 {@code ai.rag} 前缀。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.rag")
public class RagProperties {

    private boolean enabled = true;
    /** Qdrant 集合名 */
    private String collection = "products";
    /** 向量维度（百炼 text-embedding-v3 与 Qdrant 需一致） */
    private int dimension = 1024;
    /** 百炼批量 embedding 每批文本数 */
    private int embeddingBatchSize = 10;
    /** 百炼 embedding 模型 */
    private String embeddingModel = "text-embedding-v3";
    /** 百炼 API Key */
    private String embeddingApiKey;
    /** Qdrant 地址 */
    private String qdrantHost = "localhost";
    private int qdrantPort = 6333;
}
