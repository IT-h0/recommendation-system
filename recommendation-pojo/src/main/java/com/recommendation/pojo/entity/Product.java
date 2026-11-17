package com.recommendation.pojo.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
public class Product implements Serializable {

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
     * 商品图片列表(JSON)
     */
    private String images;

    /**
     * 语义标签(JSON): {"style":"复古","faceShape":"圆脸","occasion":"日常"}
     */
    private String semanticTags;

    /**
     * AI生成的商品摘要,供LLM上下文使用
     */
    private String aiSummary;

    /**
     * 状态: 1-上架, 0-下架
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
