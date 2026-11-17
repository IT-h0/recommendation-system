package com.recommendation.server.service;

import com.recommendation.pojo.dto.RatingDTO;
import com.recommendation.pojo.entity.Rating;

import java.math.BigDecimal;
import java.util.List;

public interface RatingService {

    void rate(Long userId, RatingDTO ratingDTO);

    List<Rating> getRatingsByProductId(Long productId);

    List<Rating> getRatingsByUserId(Long userId);

    List<Rating> getAllRatings();

    BigDecimal getAverageRating(Long productId);
}
