package com.recommendation.server.ai.eval;

import com.recommendation.pojo.entity.Product;
import com.recommendation.pojo.vo.ProductVO;
import com.recommendation.server.ai.rag.HybridProductSearchService;
import com.recommendation.server.mapper.ProductMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 检索评测器：对每条 Golden 查询跑混合检索，展开相关分类为商品 ID，计算各项指标。
 */
public class RetrievalEvaluator {

    private final HybridProductSearchService searchService;
    private final Map<Long, Set<Long>> categoryProducts = new HashMap<>();

    public RetrievalEvaluator(HybridProductSearchService searchService, ProductMapper productMapper) {
        this.searchService = searchService;
        for (Product p : productMapper.selectAll()) {
            if (p.getCategoryId() != null) {
                categoryProducts.computeIfAbsent(p.getCategoryId(), k -> new HashSet<>()).add(p.getId());
            }
        }
    }

    public EvaluationReport evaluate(List<GoldenQuery> queries, int topK) {
        List<EvaluationReport.PerQuery> perQuery = new ArrayList<>();

        double sumHit5 = 0, sumHit10 = 0, sumP5 = 0, sumP10 = 0;
        double sumR5 = 0, sumR10 = 0, sumMrr = 0, sumNdcg10 = 0;

        for (GoldenQuery q : queries) {
            Set<Long> relevant = expand(q.relevantCategoryIds());
            List<ProductVO> results = searchService.search(q.query(), null, null, null, topK);
            List<Long> retrieved = results.stream().map(ProductVO::getId).toList();

            double hit5 = RetrievalMetrics.hitAtK(retrieved, relevant, 5);
            double hit10 = RetrievalMetrics.hitAtK(retrieved, relevant, 10);
            double p5 = RetrievalMetrics.precisionAtK(retrieved, relevant, 5);
            double p10 = RetrievalMetrics.precisionAtK(retrieved, relevant, 10);
            double r5 = RetrievalMetrics.recallAtK(retrieved, relevant, 5);
            double r10 = RetrievalMetrics.recallAtK(retrieved, relevant, 10);
            double mrr = RetrievalMetrics.mrr(retrieved, relevant);
            double ndcg10 = RetrievalMetrics.ndcgAtK(retrieved, relevant, 10);

            sumHit5 += hit5;
            sumHit10 += hit10;
            sumP5 += p5;
            sumP10 += p10;
            sumR5 += r5;
            sumR10 += r10;
            sumMrr += mrr;
            sumNdcg10 += ndcg10;

            perQuery.add(new EvaluationReport.PerQuery(q.query(), relevant.size(), retrieved.size(), hit10, mrr));
        }

        int n = queries.size();
        return new EvaluationReport(
                n,
                round(sumHit5 / n),
                round(sumHit10 / n),
                round(sumP5 / n),
                round(sumP10 / n),
                round(sumR5 / n),
                round(sumR10 / n),
                round(sumMrr / n),
                round(sumNdcg10 / n),
                perQuery);
    }

    private Set<Long> expand(Set<Long> categoryIds) {
        Set<Long> relevant = new HashSet<>();
        for (Long cid : categoryIds) {
            Set<Long> products = categoryProducts.get(cid);
            if (products != null) {
                relevant.addAll(products);
            }
        }
        return relevant;
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
