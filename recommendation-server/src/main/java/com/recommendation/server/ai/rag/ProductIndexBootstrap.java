package com.recommendation.server.ai.rag;

import com.recommendation.pojo.entity.Category;
import com.recommendation.pojo.entity.Product;
import com.recommendation.server.mapper.CategoryMapper;
import com.recommendation.server.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 启动时加载全量商品并重建向量索引；商品库变更后可调用 {@link #rebuildIndex()} 刷新。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexBootstrap implements ApplicationRunner {

    private final ProductVectorIndex vectorIndex;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public void run(ApplicationArguments args) {
        rebuildIndex();
    }

    public void rebuildIndex() {
        try {
            List<Product> products = productMapper.selectAll();
            Map<Long, String> categoryNames = categoryMapper.selectAll().stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
            vectorIndex.rebuild(products, categoryNames);
            log.info("商品向量索引重建完成，共 {} 件商品", products.size());
        } catch (Exception e) {
            // 索引失败不阻塞应用启动：语义召回降级为关键词/全库兜底
            log.error("商品向量索引重建失败，语义召回将不可用", e);
        }
    }
}
