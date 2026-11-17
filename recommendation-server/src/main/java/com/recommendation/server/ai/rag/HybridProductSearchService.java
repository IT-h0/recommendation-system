package com.recommendation.server.ai.rag;

import com.recommendation.pojo.vo.ProductVO;
import com.recommendation.server.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 混合商品检索：稠密向量召回（语义） + 稀疏关键词召回（LIKE） + 结构化过滤（分类/价格），
 * 再用 Reciprocal Rank Fusion（RRF）融合两路召回并结合销量做重排。
 *
 * <p>关键词缺失或无召回时自动降级为「全库按销量」的兜底策略，保证 AI 导购总有结果返回。
 */
@Service
public class HybridProductSearchService {

    private static final int VECTOR_RECALL_SIZE = 50;
    /** 向量相似度阈值：低于此值的商品视为不相关，直接过滤（可调） */
    private static final double VECTOR_MIN_SCORE = 0.2;
    /** 单次最多返回商品数（一次只推荐 3 件） */
    private static final int MAX_RESULTS = 3;
    /** RRF 平滑常数 */
    private static final double RRF_K = 60.0;
    /** 销量 tie-break 权重（极小，仅用于同分时稳定排序，绝不盖过相关性） */
    private static final double SALES_BOOST_WEIGHT = 0.00005;

    private final ProductVectorIndex vectorIndex;
    private final ProductService productService;

    public HybridProductSearchService(ProductVectorIndex vectorIndex, ProductService productService) {
        this.vectorIndex = vectorIndex;
        this.productService = productService;
    }

    public List<ProductVO> search(String keyword, String categoryName,
                                  Double minPrice, Double maxPrice, int limit) {
        // 一次最多返回 MAX_RESULTS 件，避免推荐过多
        limit = Math.min(limit, MAX_RESULTS);
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String kw = hasKeyword ? keyword.trim() : null;

        Map<Long, Double> denseScores = new LinkedHashMap<>();
        List<ProductVO> keywordResults = new ArrayList<>();

        if (hasKeyword) {
            // 1) 稠密向量召回（语义）
            for (ScoredMatch m : vectorIndex.search(kw, VECTOR_RECALL_SIZE, VECTOR_MIN_SCORE)) {
                denseScores.put(m.productId(), m.score());
            }
            // 2) 稀疏关键词召回（BM25 式 LIKE，作为混合检索的另一路）
            keywordResults = productService.searchByKeyword(kw);
        }

        // 候选 id：两路召回取并集
        Set<Long> candidateIds = new LinkedHashSet<>(denseScores.keySet());
        for (ProductVO vo : keywordResults) {
            candidateIds.add(vo.getId());
        }

        // 无关键词或两路都空 → 全库兜底（销量排序由后续重排保证）
        if (candidateIds.isEmpty()) {
            keywordResults = productService.getAllProducts();
            for (ProductVO vo : keywordResults) {
                candidateIds.add(vo.getId());
            }
        }

        // 批量加载候选（含分类名/价格/销量），再做结构化过滤
        List<ProductVO> candidates = productService.getProductsByIds(new ArrayList<>(candidateIds));
        List<ProductVO> filtered = filter(candidates, categoryName, minPrice, maxPrice);

        List<ProductVO> ranked = rerank(filtered, denseScores, keywordResults);
        return ranked.size() > limit ? new ArrayList<>(ranked.subList(0, limit)) : ranked;
    }

    private List<ProductVO> filter(List<ProductVO> products, String categoryName,
                                   Double minPrice, Double maxPrice) {
        List<ProductVO> result = products;
        if (categoryName != null && !categoryName.isEmpty()) {
            result = result.stream()
                    .filter(p -> p.getCategoryName() != null && p.getCategoryName().contains(categoryName))
                    .collect(Collectors.toList());
        }
        if (minPrice != null || maxPrice != null) {
            final double min = minPrice != null ? minPrice : 0d;
            final double max = maxPrice != null ? maxPrice : Double.MAX_VALUE;
            result = result.stream()
                    .filter(p -> p.getPrice() != null
                            && p.getPrice().doubleValue() >= min
                            && p.getPrice().doubleValue() <= max)
                    .collect(Collectors.toList());
        }
        return result;
    }

    private List<ProductVO> rerank(List<ProductVO> candidates, Map<Long, Double> denseScores,
                                   List<ProductVO> keywordResults) {
        Map<Long, Integer> denseRank = rankMap(
                denseScores.entrySet().stream()
                        .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList()));
        Map<Long, Integer> keywordRank = rankMap(
                keywordResults.stream().map(ProductVO::getId).collect(Collectors.toList()));

        final int n = Math.max(1, candidates.size()) + 1;
        return candidates.stream()
                .sorted(Comparator.comparingDouble((ProductVO p) -> {
                    double rrf = 1.0 / (RRF_K + denseRank.getOrDefault(p.getId(), n))
                               + 1.0 / (RRF_K + keywordRank.getOrDefault(p.getId(), n));
                    double salesBoost = Math.log1p(p.getSales() != null ? p.getSales() : 0) * SALES_BOOST_WEIGHT;
                    return rrf + salesBoost;
                }).reversed())
                .collect(Collectors.toList());
    }

    private Map<Long, Integer> rankMap(List<Long> orderedIds) {
        Map<Long, Integer> rank = new HashMap<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            rank.putIfAbsent(orderedIds.get(i), i + 1);
        }
        return rank;
    }
}
