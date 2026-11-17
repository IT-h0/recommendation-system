package com.recommendation.server.ai.eval;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.recommendation.common.properties.AiProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM-as-judge：用 DeepSeek（fast 模型）做 RAG 生成质量评判。
 * 包含答案生成、忠实度(faithfulness)与答案相关性(answer relevance)评分。
 */
@Component
public class LlmJudge {

    private static final String CHAT_URL = "https://api.deepseek.com/chat/completions";
    /** 评测统一用 fast 模型，速度快、输出稳定（不带推理链） */
    private static final String MODEL = "deepseek-flash";

    private final RestTemplate restTemplate = new RestTemplate();
    private final AiProperties aiProperties;

    public LlmJudge(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    /** 基于检索到的商品信息生成回答 */
    public String generateAnswer(String question, String context) {
        String system = "你是电商导购助手。只能根据给定的商品信息回答用户问题，不要编造不存在的商品；回答简洁、直接。";
        String user = "商品信息：\n" + context + "\n\n用户问题：" + question;
        return chat(system, user);
    }

    /** 忠实度：回答是否完全基于给定上下文（无编造、无与上下文矛盾） */
    public double judgeFaithfulness(String question, String context, String answer) {
        String system = "你是严格的 RAG 评测员。判断 AI 回答中的每一条信息是否都能由给定的商品信息（上下文）支持，有无编造或与上下文矛盾的内容。";
        String user = "问题：" + question + "\n\n商品信息：\n" + context
                + "\n\nAI 回答：" + answer
                + "\n\n请只输出一个 0 到 1 之间的小数，表示忠实度（1=完全忠实，0=完全编造）。例如 0.85";
        return parseScore(chat(system, user));
    }

    /** 答案相关性：回答是否直接、完整地回应了问题 */
    public double judgeRelevance(String question, String answer) {
        String system = "你是 RAG 评测员。判断 AI 回答是否直接、切题地回应了用户问题，有无答非所问或遗漏关键信息。";
        String user = "问题：" + question + "\n\nAI 回答：" + answer
                + "\n\n请只输出一个 0 到 1 之间的小数，表示答案相关性（1=完全切题，0=答非所问）。例如 0.9";
        return parseScore(chat(system, user));
    }

    private String chat(String system, String user) {
        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        JSONArray messages = new JSONArray();
        JSONObject sys = new JSONObject();
        sys.put("role", "system");
        sys.put("content", system);
        JSONObject usr = new JSONObject();
        usr.put("role", "user");
        usr.put("content", user);
        messages.add(sys);
        messages.add(usr);
        body.put("messages", messages);
        body.put("temperature", 0.0);
        body.put("max_tokens", 512);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());
        HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(CHAT_URL, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("DeepSeek 调用失败: HTTP " + resp.getStatusCode());
        }
        JSONObject root = JSON.parseObject(resp.getBody());
        return root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }

    static double parseScore(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        Matcher m = Pattern.compile("\\d+(\\.\\d+)?").matcher(text);
        if (!m.find()) {
            return 0.0;
        }
        double v = Double.parseDouble(m.group());
        if (v > 1 && v <= 10) {
            v = v / 10.0;
        } else if (v > 10 && v <= 100) {
            v = v / 100.0;
        }
        return Math.max(0.0, Math.min(1.0, v));
    }
}
