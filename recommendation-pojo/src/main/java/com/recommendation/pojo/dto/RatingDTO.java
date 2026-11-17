package com.recommendation.pojo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户评分DTO
 */
@Data
public class RatingDTO {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 评分(1.0-5.0)
     */
    @NotNull(message = "评分不能为空")
    @DecimalMin(value = "1.0", message = "评分不能低于1.0")
    private BigDecimal score;

    /**
     * 评论内容
     */
    @Size(max = 500, message = "评论内容不能超过500字")
    private String comment;
}
