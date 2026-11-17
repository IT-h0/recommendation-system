package com.recommendation.server.ai.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmJudgeTest {

    @Test
    void parseScore_handlesDecimal() {
        assertThat(LlmJudge.parseScore("0.85")).isEqualTo(0.85);
        assertThat(LlmJudge.parseScore("分数：0.9")).isEqualTo(0.9);
    }

    @Test
    void parseScore_handlesTenPointScale() {
        assertThat(LlmJudge.parseScore("8.5")).isEqualTo(0.85);
    }

    @Test
    void parseScore_handlesHundredScale() {
        assertThat(LlmJudge.parseScore("85")).isEqualTo(0.85);
    }

    @Test
    void parseScore_returnsZeroForGarbage() {
        assertThat(LlmJudge.parseScore("没有数字")).isEqualTo(0.0);
        assertThat(LlmJudge.parseScore(null)).isEqualTo(0.0);
    }
}
