package com.recommendation.server.service.impl;

import com.recommendation.pojo.dto.RatingDTO;
import com.recommendation.pojo.entity.Rating;
import com.recommendation.server.mapper.RatingMapper;
import com.recommendation.server.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingMapper ratingMapper;

    @Override
    @Transactional
    public void rate(Long userId, RatingDTO ratingDTO) {
        Rating existRating = ratingMapper.selectByUserAndProduct(userId, ratingDTO.getProductId());

        if (existRating != null) {
            existRating.setScore(ratingDTO.getScore());
            existRating.setComment(ratingDTO.getComment());
            ratingMapper.update(existRating);
        } else {
            Rating rating = new Rating();
            BeanUtils.copyProperties(ratingDTO, rating);
            rating.setUserId(userId);
            ratingMapper.insert(rating);
        }
    }

    @Override
    public List<Rating> getRatingsByProductId(Long productId) {
        return ratingMapper.selectByProductId(productId);
    }

    @Override
    public List<Rating> getRatingsByUserId(Long userId) {
        return ratingMapper.selectByUserId(userId);
    }

    @Override
    public List<Rating> getAllRatings() {
        return ratingMapper.selectAll();
    }

    @Override
    public BigDecimal getAverageRating(Long productId) {
        List<Rating> ratings = ratingMapper.selectByProductId(productId);
        if (ratings == null || ratings.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = ratings.stream()
                .map(Rating::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(new BigDecimal(ratings.size()), 2, RoundingMode.HALF_UP);
    }
}
