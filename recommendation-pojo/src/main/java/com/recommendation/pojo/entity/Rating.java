package com.recommendation.pojo.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户评分实体
 */
@Data
public class Rating implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评分ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 评分(1.0-5.0)
     */
    private BigDecimal score;

    /**
     * 评论内容
     */
    private String comment;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
