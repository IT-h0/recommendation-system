package com.recommendation.server.algorithm;

import com.recommendation.pojo.entity.Product;
import com.recommendation.pojo.entity.Rating;
import com.recommendation.server.mapper.ProductMapper;
import com.recommendation.server.mapper.RatingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaborativeFilteringTest {

    @Mock
    private RatingMapper ratingMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private CollaborativeFiltering collaborativeFiltering;

    private Rating rating(long userId, long productId, String score) {
        Rating r = new Rating();
        r.setUserId(userId);
        r.setProductId(productId);
        r.setScore(new BigDecimal(score));
        return r;
    }

    private Product product(long id) {
        Product p = new Product();
        p.setId(id);
        p.setName("商品" + id);
        return p;
    }

    @Test
    void recommend_returnsCollaborativeResult_whenUserHasRatings() {
        // user1 评过 1、2；user2 评过 1、2、3
        when(ratingMapper.selectAll()).thenReturn(Arrays.asList(
                rating(1L, 1L, "5.0"),
                rating(1L, 2L, "4.0"),
                rating(2L, 1L, "4.0"),
                rating(2L, 2L, "5.0"),
                rating(2L, 3L, "3.0")
        ));

        List<CollaborativeFiltering.RecommendationResult> results =
                collaborativeFiltering.recommend(1L, 5);

        // 唯一未评分候选为商品 3
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProductId()).isEqualTo(3L);
        assertThat(results.get(0).getReason()).isEqualTo("基于协同过滤推荐");
        assertThat(results.get(0).getScore()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void recommend_fallsBackToPopular_whenNoRatingsAtAll() {
        when(ratingMapper.selectAll()).thenReturn(Collections.emptyList());
        when(productMapper.selectAll()).thenReturn(Arrays.asList(
                product(10L), product(20L), product(30L)
        ));

        List<CollaborativeFiltering.RecommendationResult> results =
                collaborativeFiltering.recommend(1L, 2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getProductId()).isEqualTo(10L);
        assertThat(results.get(0).getReason()).isEqualTo("热门推荐");
    }

    @Test
    void recommend_fallsBackToPopular_whenUserIsColdStart() {
        when(ratingMapper.selectAll()).thenReturn(Arrays.asList(
                rating(1L, 1L, "5.0"),
                rating(2L, 2L, "4.0")
        ));
        when(productMapper.selectAll()).thenReturn(Arrays.asList(product(7L), product(8L)));

        // user 3 没有任何评分 → 冷启动
        List<CollaborativeFiltering.RecommendationResult> results =
                collaborativeFiltering.recommend(3L, 1);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProductId()).isEqualTo(7L);
        assertThat(results.get(0).getReason()).isEqualTo("热门推荐");
    }
}
