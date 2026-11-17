package com.recommendation.server.algorithm;

import com.recommendation.pojo.entity.Product;
import com.recommendation.pojo.entity.Rating;
import com.recommendation.server.mapper.ProductMapper;
import com.recommendation.server.mapper.RatingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于物品的协同过滤推荐算法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollaborativeFiltering {

    private final RatingMapper ratingMapper;
    private final ProductMapper productMapper;

    /**
     * 为用户推荐商品
     * @param userId 用户ID
     * @param topN 推荐数量
     * @return 推荐商品ID列表及得分
     */
    public List<RecommendationResult> recommend(Long userId, int topN) {
        // 1. 获取所有评分数据
        List<Rating> allRatings = ratingMapper.selectAll();
        if (allRatings == null || allRatings.isEmpty()) {
            log.warn("暂无评分数据,降级为热门推荐");
            return popularFallback(topN);
        }

        // 2. 构建用户-商品评分矩阵
        Map<Long, Map<Long, BigDecimal>> userItemMatrix = buildUserItemMatrix(allRatings);

        // 3. 计算商品相似度矩阵
        Map<Long, Map<Long, BigDecimal>> itemSimilarityMatrix = calculateItemSimilarity(userItemMatrix);

        // 4. 获取用户已评分的商品
        Map<Long, BigDecimal> userRatedItems = userItemMatrix.getOrDefault(userId, Collections.emptyMap());

        // 冷启动：用户无任何评分，协同过滤无法计算，降级为热门推荐
        if (userRatedItems.isEmpty()) {
            log.warn("用户 {} 无评分记录,降级为热门推荐", userId);
            return popularFallback(topN);
        }

        // 5. 计算推荐得分
        Map<Long, BigDecimal> recommendationScores = new HashMap<>();

        for (Long targetItemId : itemSimilarityMatrix.keySet()) {
            // 跳过用户已评分的商品
            if (userRatedItems.containsKey(targetItemId)) {
                continue;
            }

            BigDecimal score = BigDecimal.ZERO;
            BigDecimal totalSimilarity = BigDecimal.ZERO;

            // 遍历用户已评分的商品
            for (Long ratedItemId : userRatedItems.keySet()) {
                BigDecimal similarity = itemSimilarityMatrix.get(ratedItemId).getOrDefault(targetItemId, BigDecimal.ZERO);

                if (similarity.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal userRating = userRatedItems.get(ratedItemId);
                    score = score.add(similarity.multiply(userRating));
                    totalSimilarity = totalSimilarity.add(similarity);
                }
            }

            // 归一化得分
            if (totalSimilarity.compareTo(BigDecimal.ZERO) > 0) {
                score = score.divide(totalSimilarity, 6, RoundingMode.HALF_UP);
                recommendationScores.put(targetItemId, score);
            }
        }

        // 6. 按得分排序,返回TopN；无候选时降级为热门推荐
        if (recommendationScores.isEmpty()) {
            return popularFallback(topN);
        }
        return recommendationScores.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> new RecommendationResult(entry.getKey(), entry.getValue(), "基于协同过滤推荐"))
                .collect(Collectors.toList());
    }

    /**
     * 冷启动/无数据时的兜底推荐：按销量返回热门商品
     */
    private List<RecommendationResult> popularFallback(int topN) {
        List<Product> products = productMapper.selectAll();
        if (products == null) {
            return Collections.emptyList();
        }
        return products.stream()
                .limit(topN)
                .map(p -> new RecommendationResult(p.getId(), BigDecimal.ZERO, "热门推荐"))
                .collect(Collectors.toList());
    }

    /**
     * 构建用户-商品评分矩阵
     */
    private Map<Long, Map<Long, BigDecimal>> buildUserItemMatrix(List<Rating> ratings) {
        Map<Long, Map<Long, BigDecimal>> matrix = new HashMap<>();

        for (Rating rating : ratings) {
            Long userId = rating.getUserId();
            Long itemId = rating.getProductId();
            BigDecimal score = rating.getScore();

            matrix.putIfAbsent(userId, new HashMap<>());
            matrix.get(userId).put(itemId, score);
        }

        return matrix;
    }

    /**
     * 计算商品相似度矩阵(基于余弦相似度)
     */
    private Map<Long, Map<Long, BigDecimal>> calculateItemSimilarity(Map<Long, Map<Long, BigDecimal>> userItemMatrix) {
        Map<Long, Map<Long, BigDecimal>> similarityMatrix = new HashMap<>();

        // 收集所有商品ID
        Set<Long> itemIds = new HashSet<>();
        for (Map<Long, BigDecimal> itemMap : userItemMatrix.values()) {
            itemIds.addAll(itemMap.keySet());
        }

        List<Long> itemList = new ArrayList<>(itemIds);

        // 计算每对商品的相似度
        for (int i = 0; i < itemList.size(); i++) {
            Long item1 = itemList.get(i);
            similarityMatrix.putIfAbsent(item1, new HashMap<>());

            for (int j = i; j < itemList.size(); j++) {
                Long item2 = itemList.get(j);

                if (item1.equals(item2)) {
                    similarityMatrix.get(item1).put(item2, BigDecimal.ONE);
                    continue;
                }

                BigDecimal similarity = calculateCosineSimilarity(userItemMatrix, item1, item2);
                similarityMatrix.get(item1).put(item2, similarity);

                similarityMatrix.putIfAbsent(item2, new HashMap<>());
                similarityMatrix.get(item2).put(item1, similarity);
            }
        }

        return similarityMatrix;
    }

    /**
     * 计算两个商品的余弦相似度
     */
    private BigDecimal calculateCosineSimilarity(Map<Long, Map<Long, BigDecimal>> userItemMatrix, Long item1, Long item2) {
        Set<Long> commonUsers = new HashSet<>();

        // 找到共同评分的用户
        for (Map.Entry<Long, Map<Long, BigDecimal>> entry : userItemMatrix.entrySet()) {
            Map<Long, BigDecimal> itemRatings = entry.getValue();
            if (itemRatings.containsKey(item1) && itemRatings.containsKey(item2)) {
                commonUsers.add(entry.getKey());
            }
        }

        if (commonUsers.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 计算余弦相似度
        BigDecimal dotProduct = BigDecimal.ZERO;
        BigDecimal norm1 = BigDecimal.ZERO;
        BigDecimal norm2 = BigDecimal.ZERO;

        for (Long userId : commonUsers) {
            Map<Long, BigDecimal> ratings = userItemMatrix.get(userId);
            BigDecimal rating1 = ratings.get(item1);
            BigDecimal rating2 = ratings.get(item2);

            dotProduct = dotProduct.add(rating1.multiply(rating2));
            norm1 = norm1.add(rating1.multiply(rating1));
            norm2 = norm2.add(rating2.multiply(rating2));
        }

        if (norm1.compareTo(BigDecimal.ZERO) == 0 || norm2.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal denominator = sqrt(norm1).multiply(sqrt(norm2));
        return dotProduct.divide(denominator, 6, RoundingMode.HALF_UP);
    }

    /**
     * 计算平方根
     */
    private BigDecimal sqrt(BigDecimal value) {
        return BigDecimal.valueOf(Math.sqrt(value.doubleValue()));
    }

    /**
     * 推荐结果
     */
    public static class RecommendationResult {
        private Long productId;
        private BigDecimal score;
        private String reason;

        public RecommendationResult(Long productId, BigDecimal score, String reason) {
            this.productId = productId;
            this.score = score;
            this.reason = reason;
        }

        public Long getProductId() {
            return productId;
        }

        public BigDecimal getScore() {
            return score;
        }

        public String getReason() {
            return reason;
        }
    }
}
