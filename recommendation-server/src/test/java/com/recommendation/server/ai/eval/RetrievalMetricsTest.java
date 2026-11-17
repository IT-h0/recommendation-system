package com.recommendation.server.ai.eval;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RetrievalMetricsTest {

    @Test
    void hitAtK_returnsOne_whenRelevantInTopK() {
        assertThat(RetrievalMetrics.hitAtK(List.of(1L, 2L, 3L), Set.of(3L), 3)).isEqualTo(1.0);
        assertThat(RetrievalMetrics.hitAtK(List.of(1L, 2L, 3L), Set.of(9L), 3)).isEqualTo(0.0);
        assertThat(RetrievalMetrics.hitAtK(List.of(), Set.of(3L), 3)).isEqualTo(0.0);
    }

    @Test
    void precisionAndRecallAtK() {
        List<Long> retrieved = List.of(1L, 2L, 3L, 4L);
        Set<Long> relevant = Set.of(1L, 3L, 5L);
        assertThat(RetrievalMetrics.precisionAtK(retrieved, relevant, 4)).isEqualTo(0.5);
        assertThat(RetrievalMetrics.recallAtK(retrieved, relevant, 4)).isCloseTo(2.0 / 3.0, within(1e-9));
    }

    @Test
    void mrr_returnsReciprocalRank() {
        assertThat(RetrievalMetrics.mrr(List.of(4L, 1L, 2L), Set.of(2L))).isCloseTo(1.0 / 3.0, within(1e-9));
        assertThat(RetrievalMetrics.mrr(List.of(4L, 1L, 2L), Set.of(9L))).isEqualTo(0.0);
    }

    @Test
    void ndcgAtK_scoresRelevantHigher() {
        // 实际结果 [2,3,1]，相关 {1,3}：3 在第 2 位、1 在第 3 位
        double ndcg = RetrievalMetrics.ndcgAtK(List.of(2L, 3L, 1L), Set.of(1L, 3L), 3);
        assertThat(ndcg).isCloseTo(0.693, within(0.001));
    }
}
