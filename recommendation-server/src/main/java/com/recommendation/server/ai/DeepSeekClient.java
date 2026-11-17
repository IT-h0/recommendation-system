package com.recommendation.server.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.recommendation.common.properties.AiProperties;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 原生 DeepSeek HTTP 客户端：直接调 /chat/completions 并携带标准 tools 参数。
 *
 * <p>动机：LangChain4j 1.0.0-beta2 与 DeepSeek 当前工具调用协议不兼容，
 * 导致 DeepSeek 返回 DSML 标记而非标准 JSON tool_calls；改用原生 HTTP 后
 * DeepSeek 返回标准 JSON，彻底根除 DSML 问题。流式输出仍走 LangChain4j。
 */
@Slf4j
@Component
public class DeepSeekClient {

    private static final String CHAT_URL = "https://api.deepseek.com/chat/completions";
    private static final String EMPTY_SCHEMA = "{\"type\":\"object\",\"properties\":{}}";

    /** 工具 JSON Schema（与 ShoppingAssistantTools 的 @Tool 定义保持一致） */
    private static final Map<String, String> TOOL_SCHEMAS = Map.of(
            "search_products",
            "{\"type\":\"object\",\"properties\":{"
                    + "\"keyword\":{\"type\":\"string\",\"description\":\"搜索关键词\"},"
                    + "\"categoryName\":{\"type\":\"string\",\"description\":\"商品分类名称\"},"
                    + "\"minPrice\":{\"type\":\"number\",\"description\":\"最低价格\"},"
                    + "\"maxPrice\":{\"type\":\"number\",\"description\":\"最高价格\"},"
                    + "\"limit\":{\"type\":\"integer\",\"description\":\"返回数量限制，默认3\"}}}",
            "get_product_detail",
            "{\"type\":\"object\",\"properties\":{"
                    + "\"productId\":{\"type\":\"integer\",\"description\":\"商品ID\"}}}",
            "get_recommendations",
            "{\"type\":\"object\",\"properties\":{"
                    + "\"topN\":{\"type\":\"integer\",\"description\":\"推荐数量，默认3\"}}}"
    );

    private final RestTemplate restTemplate = new RestTemplate();
    private final AiProperties aiProperties;

    public DeepSeekClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        log.info("DeepSeekClient 已启用（原生 HTTP 工具调用，model={}）", aiProperties.getModel());
    }

    /**
     * 一次对话轮：发送消息 + 工具定义，返回 AiMessage。
     * 若模型发起工具调用，返回带 toolExecutionRequests 的 AiMessage；否则返回纯文本 AiMessage。
     */
    public AiMessage chat(List<ChatMessage> messages, List<ToolSpecification> toolSpecs) {
        JSONObject body = new JSONObject();
        body.put("model", aiProperties.getModel());
        body.put("messages", toOpenAiMessages(messages));
        body.put("tools", toToolsJson(toolSpecs));
        body.put("tool_choice", "auto");
        body.put("temperature", 0.7);
        body.put("max_tokens", 2048);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());
        HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(CHAT_URL, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("DeepSeek 调用失败: HTTP " + resp.getStatusCode());
        }
        return parseResponse(resp.getBody());
    }

    private AiMessage parseResponse(String responseBody) {
        JSONObject root = JSON.parseObject(responseBody);
        JSONObject choice = root.getJSONArray("choices").getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");
        String content = message.getString("content");
        JSONArray toolCalls = message.getJSONArray("tool_calls");

        if (toolCalls != null && !toolCalls.isEmpty()) {
            List<ToolExecutionRequest> requests = new ArrayList<>(toolCalls.size());
            for (int i = 0; i < toolCalls.size(); i++) {
                JSONObject tc = toolCalls.getJSONObject(i);
                JSONObject fn = tc.getJSONObject("function");
                requests.add(ToolExecutionRequest.builder()
                        .id(tc.getString("id"))
                        .name(fn.getString("name"))
                        .arguments(fn.getString("arguments"))
                        .build());
            }
            return AiMessage.from(content, requests);
        }
        return AiMessage.from(content);
    }

    private JSONArray toOpenAiMessages(List<ChatMessage> messages) {
        JSONArray arr = new JSONArray();
        for (ChatMessage msg : messages) {
            switch (msg.type()) {
                case SYSTEM -> arr.add(roleMessage("system", ((SystemMessage) msg).text()));
                case USER -> arr.add(roleMessage("user", ((UserMessage) msg).singleText()));
                case AI -> arr.add(toAssistantMessage((AiMessage) msg));
                case TOOL_EXECUTION_RESULT -> arr.add(toToolResultMessage((ToolExecutionResultMessage) msg));
                default -> {
                }
            }
        }
        return arr;
    }

    private JSONObject roleMessage(String role, String content) {
        JSONObject o = new JSONObject();
        o.put("role", role);
        o.put("content", content);
        return o;
    }

    private JSONObject toAssistantMessage(AiMessage ai) {
        JSONObject o = new JSONObject();
        o.put("role", "assistant");
        o.put("content", ai.text());
        if (ai.hasToolExecutionRequests()) {
            JSONArray tcs = new JSONArray();
            for (ToolExecutionRequest tr : ai.toolExecutionRequests()) {
                JSONObject tc = new JSONObject();
                tc.put("id", tr.id());
                tc.put("type", "function");
                JSONObject fn = new JSONObject();
                fn.put("name", tr.name());
                fn.put("arguments", tr.arguments());
                tc.put("function", fn);
                tcs.add(tc);
            }
            o.put("tool_calls", tcs);
        }
        return o;
    }

    private JSONObject toToolResultMessage(ToolExecutionResultMessage tr) {
        JSONObject o = new JSONObject();
        o.put("role", "tool");
        o.put("tool_call_id", tr.id());
        o.put("content", tr.text());
        return o;
    }

    private JSONArray toToolsJson(List<ToolSpecification> toolSpecs) {
        JSONArray tools = new JSONArray();
        for (ToolSpecification spec : toolSpecs) {
            JSONObject tool = new JSONObject();
            tool.put("type", "function");
            JSONObject fn = new JSONObject();
            fn.put("name", spec.name());
            fn.put("description", spec.description());
            fn.put("parameters", JSON.parseObject(TOOL_SCHEMAS.getOrDefault(spec.name(), EMPTY_SCHEMA)));
            tool.put("function", fn);
            tools.add(tool);
        }
        return tools;
    }
}
