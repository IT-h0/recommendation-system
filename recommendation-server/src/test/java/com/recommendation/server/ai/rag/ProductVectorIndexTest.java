package com.recommendation.server.ai.rag;

import com.recommendation.pojo.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductVectorIndexTest {

    @Mock
    private TextEmbedder embedder;

    @Mock
    private QdrantProductVectorStore store;

    private ProductVectorIndex index;

    @BeforeEach
    void setUp() {
        index = new ProductVectorIndex(embedder, new ProductTextBuilder(), store);
    }

    @Test
    void rebuild_embedsDocuments_andWritesToStore() {
        Product phone = new Product();
        phone.setId(1L);
        phone.setName("iPhone 15 苹果手机");
        phone.setCategoryId(1L);
        when(embedder.embedDocuments(anyList())).thenReturn(List.of(new float[]{1f, 0f}));

        index.rebuild(List.of(phone), Map.of(1L, "手机"));

        verify(store).rebuild(eq(List.of(1L)), anyList(), anyList());
    }

    @Test
    void search_delegatesToEmbedderAndStore() {
        when(embedder.embedQuery("手机")).thenReturn(new float[]{1f, 0f});
        when(store.search(any(float[].class), eq(3), eq(0.0)))
                .thenReturn(List.of(new ScoredMatch(1L, 0.9)));

        List<ScoredMatch> results = index.search("手机", 3, 0.0);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).productId()).isEqualTo(1L);
    }

    @Test
    void search_returnsEmpty_whenEmbedderFails() {
        when(embedder.embedQuery("手机")).thenThrow(new RuntimeException("dashscope down"));

        assertThat(index.search("手机", 3, 0.0)).isEmpty();
    }
}
