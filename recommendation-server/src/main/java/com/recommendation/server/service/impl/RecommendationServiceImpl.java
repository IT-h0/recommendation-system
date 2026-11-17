package com.recommendation.server.service.impl;

import com.recommendation.pojo.entity.Product;
import com.recommendation.pojo.vo.RecommendationVO;
import com.recommendation.server.algorithm.CollaborativeFiltering;
import com.recommendation.server.mapper.ProductMapper;
import com.recommendation.server.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final CollaborativeFiltering collaborativeFiltering;
    private final ProductMapper productMapper;

    @Override
    public List<RecommendationVO> recommend(Long userId, int topN) {
        List<CollaborativeFiltering.RecommendationResult> results =
                collaborativeFiltering.recommend(userId, topN);

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询，避免对每个推荐结果逐条 selectById 造成 N+1
        List<Long> ids = results.stream()
                .map(CollaborativeFiltering.RecommendationResult::getProductId)
                .collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return results.stream()
                .map(result -> {
                    Product product = productMap.get(result.getProductId());
                    if (product == null) {
                        return null;
                    }

                    RecommendationVO vo = new RecommendationVO();
                    vo.setProductId(product.getId());
                    vo.setProductName(product.getName());
                    vo.setProductImage(product.getImageUrl());
                    vo.setPrice(product.getPrice());
                    vo.setScore(result.getScore());
                    vo.setReason(result.getReason());

                    return vo;
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());
    }
}
