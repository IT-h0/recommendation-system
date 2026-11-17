package com.recommendation.server.ai.rag;

import java.util.List;

/**
 * 文本嵌入抽象：把文本映射为稠密向量。
 * 当前实现为百炼(DashScope) text-embedding；可替换为 BGE-ONNX / OpenAI 等，
 * 上层检索逻辑无需改动。
 */
public interface TextEmbedder {

    /** 批量嵌入（索引用，document 语义） */
    List<float[]> embedDocuments(List<String> texts);

    /** 单条嵌入（查询用，query 语义） */
    float[] embedQuery(String text);
}
