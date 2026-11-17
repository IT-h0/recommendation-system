package com.recommendation.server.ai.tool;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingAssistantToolsTest {

    @Test
    void extractArgsFromDsml_parsesParameters() {
        // 模拟 DeepSeek 返回的 DSML 工具参数（非标准 JSON）
        String dsml = "<｜DSML｜invoke name=\"search_products\">\n" +
                "<｜DSML｜parameter name=\"keyword\" string=\"true\">复古 墨镜</｜DSML｜parameter>\n" +
                "<｜DSML｜parameter name=\"categoryName\" string=\"true\">服装鞋帽</｜DSML｜parameter>\n" +
                "<｜DSML｜parameter name=\"minPrice\" string=\"false\">0</｜DSML｜parameter>\n" +
                "<｜DSML｜parameter name=\"maxPrice\" string=\"false\">600</｜DSML｜parameter>\n" +
                "<｜DSML｜parameter name=\"limit\" string=\"false\">10</｜DSML｜parameter>\n" +
                "</｜DSML｜invoke>";

        JSONObject args = new ShoppingAssistantTools(null, null, null, null)
                .extractArgsFromDsml(dsml);

        assertThat(args.getString("keyword")).isEqualTo("复古 墨镜");
        assertThat(args.getString("categoryName")).isEqualTo("服装鞋帽");
        assertThat(args.getDouble("minPrice")).isEqualTo(0.0);
        assertThat(args.getDouble("maxPrice")).isEqualTo(600.0);
        assertThat(args.getIntValue("limit")).isEqualTo(10);
    }

    @Test
    void extractArgsFromDsml_handlesJsonMixedContent() {
        // 模拟 JSON 与 DSML 混杂的畸形输出
        String mixed = "{\"keyword\": \"牛仔裤</｜DSML｜parameter>\n" +
                "<｜DSML｜parameter name=\"categoryName\" string=\"true\">服装鞋帽>牛仔裤</｜DSML｜parameter>\n" +
                "<｜DSML｜parameter name=\"maxPrice\" string=\"false\">600</｜DSML｜parameter>";

        JSONObject args = new ShoppingAssistantTools(null, null, null, null)
                .extractArgsFromDsml(mixed);

        assertThat(args.getString("categoryName")).isEqualTo("服装鞋帽>牛仔裤");
        assertThat(args.getDouble("maxPrice")).isEqualTo(600.0);
    }
}
