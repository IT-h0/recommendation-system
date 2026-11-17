package com.recommendation.server.controller;

import com.recommendation.common.result.Result;
import com.recommendation.pojo.vo.RecommendationVO;
import com.recommendation.server.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐Controller
 */
@Tag(name = "商品推荐", description = "协同过滤推荐接口")
@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * 为用户推荐商品
     */
    @Operation(summary = "为用户推荐商品")
    @GetMapping
    public Result<List<RecommendationVO>> recommend(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "推荐数量")
            @RequestParam(defaultValue = "10") Integer topN) {
        List<RecommendationVO> recommendations = recommendationService.recommend(userId, topN);
        return Result.success(recommendations);
    }
}
