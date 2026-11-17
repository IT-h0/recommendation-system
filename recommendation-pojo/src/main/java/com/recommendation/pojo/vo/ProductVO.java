package com.recommendation.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品VO
 */
@Data
public class ProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 主图URL
     */
    private String imageUrl;

    /**
     * 平均评分
     */
    private BigDecimal averageRating;

    /**
     * 评分人数
     */
    private Integer ratingCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
