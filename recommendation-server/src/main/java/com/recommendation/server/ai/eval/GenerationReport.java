package com.recommendation.server.ai.eval;

import java.util.List;

/**
 * 生成质量评测报告：忠实度 + 答案相关性（0-1，越高越好）。
 */
public record GenerationReport(
        int queryCount,
        double faithfulness,
        double answerRelevance,
        List<PerQuery> perQuery) {

    public record PerQuery(String query, double faithfulness, double relevance, String answerPreview) {
    }

    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# RAG 生成质量评测报告\n\n");
        sb.append("## 聚合指标\n\n");
        sb.append("| 指标 | 值 |\n|---|---|\n");
        sb.append(String.format("| 查询数 | %d |%n", queryCount));
        sb.append(String.format("| 忠实度 (Faithfulness) | %.3f |%n", faithfulness));
        sb.append(String.format("| 答案相关性 (Answer Relevance) | %.3f |%n", answerRelevance));
        sb.append("\n## 明细\n\n");
        sb.append("| 查询 | 忠实度 | 相关性 | 回答摘要 |\n|---|---|---|---|\n");
        for (PerQuery p : perQuery) {
            String preview = p.answerPreview() == null ? "" : p.answerPreview().replace("|", "｜");
            if (preview.length() > 40) {
                preview = preview.substring(0, 40) + "…";
            }
            sb.append(String.format("| %s | %.2f | %.2f | %s |%n",
                    p.query(), p.faithfulness(), p.relevance(), preview));
        }
        return sb.toString();
    }
}
