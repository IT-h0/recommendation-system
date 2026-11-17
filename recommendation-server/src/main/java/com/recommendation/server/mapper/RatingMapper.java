package com.recommendation.server.mapper;

import com.recommendation.pojo.entity.Rating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评分Mapper
 */
@Mapper
public interface RatingMapper {

    /**
     * 根据用户ID和商品ID查询评分
     */
    Rating selectByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 根据商品ID查询所有评分
     */
    List<Rating> selectByProductId(@Param("productId") Long productId);

    /**
     * 根据用户ID查询所有评分
     */
    List<Rating> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询所有评分
     */
    List<Rating> selectAll();

    /**
     * 插入评分
     */
    int insert(Rating rating);

    /**
     * 更新评分
     */
    int update(Rating rating);
}
