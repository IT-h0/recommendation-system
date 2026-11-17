package com.recommendation.server.ai.eval;

import com.recommendation.pojo.vo.ProductVO;
import com.recommendation.server.ai.rag.HybridProductSearchService;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成质量评测器：检索 -> 生成 -> LLM-as-judge 评判（忠实度 + 相关性）。
 */
public class GenerationEvaluator {

    private final HybridProductSearchService searchService;
    private final LlmJudge judge;

    public GenerationEvaluator(HybridProductSearchService searchService, LlmJudge judge) {
        this.searchService = searchService;
        this.judge = judge;
    }

    public GenerationReport evaluate(List<GoldenQuery> queries, int topK) {
        List<GenerationReport.PerQuery> perQuery = new ArrayList<>();
        double sumFaith = 0, sumRel = 0;

        for (GoldenQuery q : queries) {
            // 1. 检索 top-K 商品作为上下文
            List<ProductVO> products = searchService.search(q.query(), null, null, null, topK);
            String context = formatContext(products);

            // 2. 生成回答
            String answer = judge.generateAnswer(q.query(), context);

            // 3. LLM-as-judge 评分
            double faith = judge.judgeFaithfulness(q.query(), context, answer);
            double rel = judge.judgeRelevance(q.query(), answer);

            sumFaith += faith;
            sumRel += rel;
            perQuery.add(new GenerationReport.PerQuery(q.query(), round(faith), round(rel), answer));
        }

        int n = queries.size();
        return new GenerationReport(n, round(sumFaith / n), round(sumRel / n), perQuery);
    }

    private String formatContext(List<ProductVO> products) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < products.size(); i++) {
            ProductVO p = products.get(i);
            sb.append(i + 1).append(". ").append(p.getName());
            if (p.getPrice() != null) {
                sb.append(" - ¥").append(p.getPrice());
            }
            if (p.getDescription() != null && !p.getDescription().isEmpty()) {
                sb.append(" - ").append(p.getDescription());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
