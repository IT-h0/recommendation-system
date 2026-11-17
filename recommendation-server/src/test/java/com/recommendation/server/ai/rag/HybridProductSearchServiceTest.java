package com.recommendation.server.ai.rag;

import com.recommendation.pojo.vo.ProductVO;
import com.recommendation.server.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridProductSearchServiceTest {

    @Mock
    private ProductVectorIndex vectorIndex;

    @Mock
    private ProductService productService;

    @InjectMocks
    private HybridProductSearchService service;

    @Test
    void search_mergesDenseAndKeyword_andRanksByRelevance() {
        when(vectorIndex.search("手机", 50, 0.2)).thenReturn(List.of(
                new ScoredMatch(1L, 0.9),
                new ScoredMatch(2L, 0.5)));
        when(productService.searchByKeyword("手机")).thenReturn(List.of(vo(1L), vo(3L)));
        when(productService.getProductsByIds(anyList())).thenReturn(List.of(vo(1L), vo(2L), vo(3L)));

        List<ProductVO> results = service.search("手机", null, null, null, 10);

        assertThat(results).hasSize(3);
        // 商品1 同时命中向量高分 + 关键词，RRF 后应排第一
        assertThat(results.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void search_fallsBackToAll_whenNoKeyword() {
        when(productService.getAllProducts()).thenReturn(List.of(vo(10L), vo(20L)));
        when(productService.getProductsByIds(anyList())).thenReturn(List.of(vo(10L), vo(20L)));

        List<ProductVO> results = service.search(null, null, null, null, 10);

        assertThat(results).hasSize(2);
    }

    @Test
    void search_appliesPriceFilter() {
        when(vectorIndex.search("手机", 50, 0.2)).thenReturn(List.of(new ScoredMatch(1L, 0.9)));
        when(productService.searchByKeyword("手机")).thenReturn(List.of());
        when(productService.getProductsByIds(anyList())).thenReturn(List.of(vo(1L, 9999.0), vo(2L, 100.0)));

        List<ProductVO> results = service.search("手机", null, 0.0, 500.0, 10);

        // 价格 9999 的被过滤，只剩 100 的商品
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(2L);
    }

    private ProductVO vo(Long id) {
        return vo(id, 100.0);
    }

    private ProductVO vo(Long id, double price) {
        ProductVO vo = new ProductVO();
        vo.setId(id);
        vo.setName("商品" + id);
        vo.setPrice(new BigDecimal(String.valueOf(price)));
        vo.setSales(10);
        return vo;
    }
}
