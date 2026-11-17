package com.recommendation.server.ai.eval;

import java.util.List;

/**
 * 评测报告：聚合指标 + 每条查询的明细。
 */
public record EvaluationReport(
        int queryCount,
        double hitAt5,
        double hitAt10,
        double precisionAt5,
        double precisionAt10,
        double recallAt5,
        double recallAt10,
        double mrr,
        double ndcgAt10,
        List<PerQuery> perQuery) {

    public record PerQuery(String query, int relevantCount, int retrievedCount, double hitAt10, double mrr) {
    }

    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# RAG 检索评测报告\n\n");
        sb.append("## 聚合指标（Top-5 / Top-10）\n\n");
        sb.append("| 指标 | 值 |\n|---|---|\n");
        sb.append(String.format("| 查询数 | %d |%n", queryCount));
        sb.append(String.format("| Hit@5 | %.3f |%n", hitAt5));
        sb.append(String.format("| Hit@10 | %.3f |%n", hitAt10));
        sb.append(String.format("| Precision@5 | %.3f |%n", precisionAt5));
        sb.append(String.format("| Precision@10 | %.3f |%n", precisionAt10));
        sb.append(String.format("| Recall@5 | %.3f |%n", recallAt5));
        sb.append(String.format("| Recall@10 | %.3f |%n", recallAt10));
        sb.append(String.format("| MRR | %.3f |%n", mrr));
        sb.append(String.format("| NDCG@10 | %.3f |%n", ndcgAt10));
        sb.append("\n## 明细\n\n");
        sb.append("| 查询 | 相关数 | 召回数 | Hit@10 | MRR |\n|---|---|---|---|---|\n");
        for (PerQuery p : perQuery) {
            sb.append(String.format("| %s | %d | %d | %.2f | %.3f |%n",
                    p.query(), p.relevantCount(), p.retrievedCount(), p.hitAt10(), p.mrr()));
        }
        return sb.toString();
    }
}
