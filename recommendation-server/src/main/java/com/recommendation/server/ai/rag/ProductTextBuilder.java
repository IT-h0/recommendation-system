package com.recommendation.server.ai.rag;

import com.recommendation.pojo.entity.Product;
import org.springframework.stereotype.Component;

/**
 * 构建用于向量化/检索的商品文本：拼接名称、分类、描述、语义标签与 AI 摘要，
 * 使商品的语义信息（如 {@code semantic_tags} 里的 "复古/圆脸"）进入检索空间。
 */
@Component
public class ProductTextBuilder {

    public String build(Product product, String categoryName) {
        StringBuilder sb = new StringBuilder();
        if (product.getName() != null) {
            sb.append(product.getName()).append(' ');
        }
        if (categoryName != null && !categoryName.isEmpty()) {
            sb.append(categoryName).append(' ');
        }
        if (product.getDescription() != null) {
            sb.append(product.getDescription()).append(' ');
        }
        if (product.getSemanticTags() != null) {
            sb.append(product.getSemanticTags()).append(' ');
        }
        if (product.getAiSummary() != null) {
            sb.append(product.getAiSummary());
        }
        return sb.toString().trim();
    }
}
