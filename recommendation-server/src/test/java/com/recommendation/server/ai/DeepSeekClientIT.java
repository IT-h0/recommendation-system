package com.recommendation.server.ai;

import com.recommendation.common.properties.AiProperties;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 真实调用 DeepSeek 验证 DeepSeekClient 返回标准 JSON tool_calls（无 DSML）。
 * 运行方式：mvn -Dtest=DeepSeekClientIT -Dsurefire.failIfNoSpecifiedTests=false test
 */
class DeepSeekClientIT {

    @Test
    void chat_returnsStandardToolCalls() {
        AiProperties props = new AiProperties();
        props.setApiKey("sk-56318ae09dfa49d691d7d4c13eed8b91");
        props.setModel("deepseek-flash");

        DeepSeekClient client = new DeepSeekClient(props);

        ToolSpecification spec = mock(ToolSpecification.class);
        when(spec.name()).thenReturn("search_products");
        when(spec.description()).thenReturn("搜索商品");

        AiMessage result = client.chat(
                List.of(UserMessage.from("帮我找手机")),
                List.of(spec));

        System.out.println("hasToolCalls=" + result.hasToolExecutionRequests());
        if (result.hasToolExecutionRequests()) {
            for (ToolExecutionRequest tr : result.toolExecutionRequests()) {
                System.out.println("tool=" + tr.name() + " args=" + tr.arguments());
            }
        }

        assertThat(result.hasToolExecutionRequests()).isTrue();
        assertThat(result.toolExecutionRequests().get(0).name()).isEqualTo("search_products");
    }
}
