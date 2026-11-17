package com.recommendation.server.ai.rag;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Qdrant 向量库实现：通过官方 REST 接口完成「建集合 / 批量 upsert / 相似度搜索」。
 * 使用 Cosine 距离，point id 即商品 ID。
 */
@Slf4j
@Component
public class QdrantProductVectorStore {

    private final RestTemplate restTemplate = new RestTemplate();
    private final RagProperties properties;

    public QdrantProductVectorStore(RagProperties properties) {
        this.properties = properties;
    }

    private String baseUrl() {
        return "http://" + properties.getQdrantHost() + ":" + properties.getQdrantPort();
    }

    /** 全量重建：删除旧集合 → 建集合 → 批量 upsert */
    public void rebuild(List<Long> ids, List<float[]> vectors, List<String> texts) {
        String collection = properties.getCollection();
        deleteCollection(collection);

        JSONObject create = new JSONObject();
        JSONObject vectorsCfg = new JSONObject();
        vectorsCfg.put("size", properties.getDimension());
        vectorsCfg.put("distance", "Cosine");
        create.put("vectors", vectorsCfg);
        putJson("/collections/" + collection, create);

        if (ids != null && !ids.isEmpty()) {
            JSONArray points = new JSONArray();
            for (int i = 0; i < ids.size(); i++) {
                JSONObject point = new JSONObject();
                point.put("id", ids.get(i));
                point.put("vector", vectors.get(i));
                JSONObject payload = new JSONObject();
                payload.put("text", texts.get(i));
                point.put("payload", payload);
                points.add(point);
            }
            JSONObject upsert = new JSONObject();
            upsert.put("points", points);
            putJson("/collections/" + collection + "/points?wait=true", upsert);
        }
        log.info("Qdrant 索引重建完成: 集合={}, 点数={}", collection, ids == null ? 0 : ids.size());
    }

    /** 余弦相似度搜索，返回 (productId, score)，score 越高越相关 */
    public List<ScoredMatch> search(float[] queryVector, int limit, double minScore) {
        JSONObject body = new JSONObject();
        body.put("vector", queryVector);
        body.put("limit", limit);
        body.put("with_payload", false);
        body.put("with_vector", false);
        String resp = postJson("/collections/" + properties.getCollection() + "/points/search", body);

        JSONArray results = JSON.parseObject(resp).getJSONArray("result");
        List<ScoredMatch> matches = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            JSONObject r = results.getJSONObject(i);
            double score = r.getDoubleValue("score");
            if (score < minScore) {
                continue;
            }
            matches.add(new ScoredMatch(r.getLongValue("id"), score));
        }
        return matches;
    }

    private void deleteCollection(String collection) {
        try {
            restTemplate.exchange(baseUrl() + "/collections/" + collection,
                    HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
        } catch (Exception ignored) {
            // 集合不存在时忽略
        }
    }

    private void putJson(String path, JSONObject body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);
        ResponseEntity<String> resp = restTemplate.exchange(baseUrl() + path, HttpMethod.PUT, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Qdrant PUT 失败: " + path + " -> " + resp.getStatusCode());
        }
    }

    private String postJson(String path, JSONObject body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);
        ResponseEntity<String> resp = restTemplate.exchange(baseUrl() + path, HttpMethod.POST, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Qdrant POST 失败: " + path + " -> " + resp.getStatusCode());
        }
        return resp.getBody();
    }
}
