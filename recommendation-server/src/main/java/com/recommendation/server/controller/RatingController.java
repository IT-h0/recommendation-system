package com.recommendation.server.controller;

import com.recommendation.common.result.Result;
import com.recommendation.pojo.dto.RatingDTO;
import com.recommendation.pojo.entity.Rating;
import com.recommendation.server.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评分Controller
 */
@Tag(name = "评分管理", description = "用户评分接口")
@RestController
@RequestMapping("/api/rating")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    /**
     * 用户评分
     */
    @Operation(summary = "用户评分")
    @PostMapping
    public Result<Void> rate(@RequestAttribute("userId") Long userId,
                             @Valid @RequestBody RatingDTO ratingDTO) {
        ratingService.rate(userId, ratingDTO);
        return Result.success();
    }

    /**
     * 查询商品的所有评分
     */
    @Operation(summary = "查询商品的所有评分")
    @GetMapping("/product/{productId}")
    public Result<List<Rating>> getRatingsByProductId(@PathVariable Long productId) {
        List<Rating> ratings = ratingService.getRatingsByProductId(productId);
        return Result.success(ratings);
    }

    /**
     * 查询用户的所有评分
     */
    @Operation(summary = "查询用户的所有评分")
    @GetMapping("/user")
    public Result<List<Rating>> getRatingsByUserId(@RequestAttribute("userId") Long userId) {
        List<Rating> ratings = ratingService.getRatingsByUserId(userId);
        return Result.success(ratings);
    }
}
