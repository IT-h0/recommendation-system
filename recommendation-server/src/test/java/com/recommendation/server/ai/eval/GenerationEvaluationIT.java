package com.recommendation.server.ai.eval;

import com.recommendation.pojo.entity.Category;
import com.recommendation.pojo.entity.Product;
import com.recommendation.server.ai.rag.HybridProductSearchService;
import com.recommendation.server.ai.rag.ProductVectorIndex;
import com.recommendation.server.mapper.CategoryMapper;
import com.recommendation.server.mapper.ProductMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG 生成质量评测（LLM-as-judge，@Tag("eval")，默认不随 mvn test 运行）。
 * 运行方式：mvn -Dtest=GenerationEvaluationIT test
 * 前置：MySQL、Qdrant、百炼、DeepSeek 均可用。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("eval")
class GenerationEvaluationIT {

    @Autowired
    private HybridProductSearchService searchService;

    @Autowired
    private LlmJudge judge;

    @Autowired
    private ProductVectorIndex vectorIndex;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Test
    void runGenerationEvaluation() {
        // 1. 重建向量索引，保证评测可复现
        List<Product> products = productMapper.selectAll();
        Map<Long, String> categoryNames = categoryMapper.selectAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        vectorIndex.rebuild(products, categoryNames);

        // 2. 检索 -> 生成 -> 评判
        GenerationEvaluator evaluator = new GenerationEvaluator(searchService, judge);
        GenerationReport report = evaluator.evaluate(GoldenDataset.standard(), 5);
        System.out.println(report.toMarkdown());

        // 3. 基本质量断言
        assertThat(report.faithfulness()).isGreaterThan(0.5);
        assertThat(report.answerRelevance()).isGreaterThan(0.5);
    }
}
