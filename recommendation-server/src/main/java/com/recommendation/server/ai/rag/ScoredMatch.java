package com.recommendation.server.ai.rag;

/** 向量检索命中：商品 ID + 相似度分数 */
public record ScoredMatch(Long productId, Double score) {
}
