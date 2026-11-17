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
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG 检索评测（集成测试，@Tag("eval")，默认不随 mvn test 运行）。
 * 运行方式：mvn -Dtest=RetrievalEvaluationIT test
 * 前置：MySQL、Qdrant、百炼 embedding 均可用。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("eval")
class RetrievalEvaluationIT {

    @Autowired
    private HybridProductSearchService searchService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductVectorIndex vectorIndex;

    @Test
    void runEvaluation() {
        // 1. 重建向量索引，保证评测可复现
        List<Product> products = productMapper.selectAll();
        Map<Long, String> categoryNames = categoryMapper.selectAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        vectorIndex.rebuild(products, categoryNames);

        // 2. Golden 评测集：语义查询 -> 相关分类（分类下全部商品视为相关）
        List<GoldenQuery> golden = GoldenDataset.standard();

        // 3. 跑评测并输出报告
        RetrievalEvaluator evaluator = new RetrievalEvaluator(searchService, productMapper);
        EvaluationReport report = evaluator.evaluate(golden, 10);
        System.out.println(report.toMarkdown());

        // 4. 基本质量断言（语义检索应达到的底线）
        assertThat(report.hitAt10()).isGreaterThan(0.5);
        assertThat(report.mrr()).isGreaterThan(0.3);
    }
}
