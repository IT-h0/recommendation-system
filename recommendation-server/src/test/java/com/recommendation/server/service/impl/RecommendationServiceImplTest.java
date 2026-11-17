package com.recommendation.server.service.impl;

import com.recommendation.pojo.entity.Product;
import com.recommendation.pojo.vo.RecommendationVO;
import com.recommendation.server.algorithm.CollaborativeFiltering;
import com.recommendation.server.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private CollaborativeFiltering collaborativeFiltering;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @Test
    void recommend_returnsEmpty_whenNoResults() {
        when(collaborativeFiltering.recommend(1L, 10)).thenReturn(Collections.emptyList());

        List<RecommendationVO> vos = recommendationService.recommend(1L, 10);

        assertThat(vos).isEmpty();
    }

    @Test
    void recommend_mapsProducts_andFiltersMissing() {
        CollaborativeFiltering.RecommendationResult r1 =
                new CollaborativeFiltering.RecommendationResult(1L, new BigDecimal("4.5"), "基于协同过滤推荐");
        CollaborativeFiltering.RecommendationResult r2 =
                new CollaborativeFiltering.RecommendationResult(2L, new BigDecimal("4.0"), "基于协同过滤推荐");
        when(collaborativeFiltering.recommend(1L, 10)).thenReturn(List.of(r1, r2));

        Product p1 = new Product();
        p1.setId(1L);
        p1.setName("商品1");
        p1.setImageUrl("http://img/1.jpg");
        p1.setPrice(new BigDecimal("99.00"));
        // 只返回 id=1 的商品，模拟 id=2 已下架/被删除，验证批量查询后被过滤
        when(productMapper.selectByIds(anyList())).thenReturn(List.of(p1));

        List<RecommendationVO> vos = recommendationService.recommend(1L, 10);

        assertThat(vos).hasSize(1);
        assertThat(vos.get(0).getProductId()).isEqualTo(1L);
        assertThat(vos.get(0).getProductName()).isEqualTo("商品1");
        assertThat(vos.get(0).getScore()).isEqualByComparingTo("4.5");
    }
}
