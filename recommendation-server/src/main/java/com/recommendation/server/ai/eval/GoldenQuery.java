package com.recommendation.server.ai.eval;

import java.util.Set;

/**
 * Golden 评测条目：一个语义化的查询 + 其相关商品分类集合。
 * 相关商品 = 这些分类下的全部商品，由评测器在运行期从数据库展开。
 */
public record GoldenQuery(String query, Set<Long> relevantCategoryIds) {
}
