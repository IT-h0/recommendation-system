package com.recommendation.server.service;

import com.recommendation.pojo.vo.RecommendationVO;

import java.util.List;

public interface RecommendationService {

    List<RecommendationVO> recommend(Long userId, int topN);
}
