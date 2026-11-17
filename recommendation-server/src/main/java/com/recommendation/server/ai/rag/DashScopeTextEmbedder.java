package com.recommendation.server.ai.rag;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 百炼(DashScope) text-embedding-v3 嵌入实现，通过官方 REST 接口调用。
 * 索引用 document 语义、查询用 query 语义，以提升检索质量。
 */
@Component
public class DashScopeTextEmbedder implements TextEmbedder {

    private static final String EMBEDDING_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";

    private final RestTemplate restTemplate = new RestTemplate();
    private final RagProperties properties;

    public DashScopeTextEmbedder(RagProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        List<float[]> result = new ArrayList<>(texts.size());
        int batch = Math.max(1, properties.getEmbeddingBatchSize());
        for (int i = 0; i < texts.size(); i += batch) {
            List<String> chunk = texts.subList(i, Math.min(i + batch, texts.size()));
            result.addAll(embed(chunk, "document"));
        }
        return result;
    }

    @Override
    public float[] embedQuery(String text) {
        List<float[]> result = embed(Collections.singletonList(text), "query");
        return result.isEmpty() ? new float[0] : result.get(0);
    }

    private List<float[]> embed(List<String> texts, String textType) {
        JSONObject body = new JSONObject();
        body.put("model", properties.getEmbeddingModel());
        JSONObject input = new JSONObject();
        input.put("texts", texts);
        body.put("input", input);
        JSONObject params = new JSONObject();
        params.put("text_type", textType);
        if (properties.getDimension() > 0) {
            params.put("dimension", properties.getDimension());
        }
        body.put("parameters", params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getEmbeddingApiKey());
        HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(EMBEDDING_URL, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("百炼 embedding 调用失败: HTTP " + resp.getStatusCode());
        }
        return parseEmbeddings(resp.getBody());
    }

    private List<float[]> parseEmbeddings(String responseBody) {
        JSONObject root = JSON.parseObject(responseBody);
        JSONObject output = root.getJSONObject("output");
        if (output == null) {
            throw new IllegalStateException("百炼 embedding 响应缺少 output: " + responseBody);
        }
        JSONArray embeddings = output.getJSONArray("embeddings");
        // 按 text_index 排序，保证与输入顺序一致
        Map<Integer, float[]> byIndex = new TreeMap<>();
        for (int i = 0; i < embeddings.size(); i++) {
            JSONObject e = embeddings.getJSONObject(i);
            int idx = e.getIntValue("text_index");
            JSONArray arr = e.getJSONArray("embedding");
            float[] vec = new float[arr.size()];
            for (int j = 0; j < arr.size(); j++) {
                vec[j] = arr.getFloatValue(j);
            }
            byIndex.put(idx, vec);
        }
        return new ArrayList<>(byIndex.values());
    }
}
