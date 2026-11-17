package com.recommendation.server.ai.eval;

import java.util.List;
import java.util.Set;

/**
 * 检索评测指标（纯函数，无外部依赖，可单测）。
 * 采用二值相关性：商品要么相关、要么不相关。
 */
public final class RetrievalMetrics {

    private RetrievalMetrics() {
    }

    /** Hit@K：前 K 个结果中是否至少命中一个相关商品（0 或 1） */
    public static double hitAtK(List<Long> retrieved, Set<Long> relevant, int k) {
        if (relevant.isEmpty() || retrieved.isEmpty()) {
            return 0.0;
        }
        int n = Math.min(k, retrieved.size());
        for (int i = 0; i < n; i++) {
            if (relevant.contains(retrieved.get(i))) {
                return 1.0;
            }
        }
        return 0.0;
    }

    /** Precision@K：前 K 个结果中相关商品占比 */
    public static double precisionAtK(List<Long> retrieved, Set<Long> relevant, int k) {
        if (retrieved.isEmpty()) {
            return 0.0;
        }
        int n = Math.min(k, retrieved.size());
        int hits = countHits(retrieved, relevant, n);
        return (double) hits / n;
    }

    /** Recall@K：前 K 个结果中命中的相关商品 / 全部相关商品 */
    public static double recallAtK(List<Long> retrieved, Set<Long> relevant, int k) {
        if (relevant.isEmpty()) {
            return 0.0;
        }
        int n = Math.min(k, retrieved.size());
        int hits = countHits(retrieved, relevant, n);
        return (double) hits / relevant.size();
    }

    /** MRR：第一个相关商品排名的倒数 */
    public static double mrr(List<Long> retrieved, Set<Long> relevant) {
        if (relevant.isEmpty()) {
            return 0.0;
        }
        for (int i = 0; i < retrieved.size(); i++) {
            if (relevant.contains(retrieved.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    /** NDCG@K：折损累计增益（二值相关性，DCG 用 log2 折损） */
    public static double ndcgAtK(List<Long> retrieved, Set<Long> relevant, int k) {
        if (relevant.isEmpty()) {
            return 0.0;
        }
        int n = Math.min(k, retrieved.size());
        double dcg = 0.0;
        for (int i = 0; i < n; i++) {
            if (relevant.contains(retrieved.get(i))) {
                dcg += 1.0 / log2(i + 2);
            }
        }
        int idealCount = Math.min(k, relevant.size());
        double idcg = 0.0;
        for (int i = 0; i < idealCount; i++) {
            idcg += 1.0 / log2(i + 2);
        }
        return idcg == 0.0 ? 0.0 : dcg / idcg;
    }

    private static int countHits(List<Long> retrieved, Set<Long> relevant, int n) {
        int hits = 0;
        for (int i = 0; i < n; i++) {
            if (relevant.contains(retrieved.get(i))) {
                hits++;
            }
        }
        return hits;
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}
