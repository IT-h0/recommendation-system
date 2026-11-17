package com.recommendation.server.ai.rag;

import com.recommendation.pojo.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 商品向量索引门面：负责「商品文本构建 → 嵌入 → 写入向量库」与「查询向量 → 语义检索」的编排。
 * 嵌入层（{@link TextEmbedder}）与向量库（{@link QdrantProductVectorStore}）均可独立替换。
 */
@Slf4j
@Component
public class ProductVectorIndex {

    private final TextEmbedder embedder;
    private final ProductTextBuilder textBuilder;
    private final QdrantProductVectorStore store;

    public ProductVectorIndex(TextEmbedder embedder,
                              ProductTextBuilder textBuilder,
                              QdrantProductVectorStore store) {
        this.embedder = embedder;
        this.textBuilder = textBuilder;
        this.store = store;
    }

    /** 全量重建索引（商品变更时调用） */
    public void rebuild(List<Product> products, Map<Long, String> categoryNames) {
        if (products == null || products.isEmpty()) {
            store.rebuild(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            return;
        }
        List<String> texts = new ArrayList<>(products.size());
        List<Long> ids = new ArrayList<>(products.size());
        for (Product p : products) {
            ids.add(p.getId());
            texts.add(textBuilder.build(p, categoryNames.get(p.getCategoryId())));
        }
        List<float[]> vectors = embedder.embedDocuments(texts);
        store.rebuild(ids, vectors, texts);
    }

    /** 语义检索，返回按相似度降序的 (productId, score) 列表 */
    public List<ScoredMatch> search(String query, int maxResults, double minScore) {
        try {
            float[] queryVector = embedder.embedQuery(query);
            return store.search(queryVector, maxResults, minScore);
        } catch (Exception e) {
            // 向量检索不可用时降级为空结果，由上层混合检索回退到关键词/全库兜底
            log.warn("向量检索失败，降级为关键词/全库检索: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
