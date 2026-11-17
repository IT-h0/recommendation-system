package com.recommendation.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ChatMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String role;
    private String content;
    private String metadata;
    private LocalDateTime createdTime;

    /**
     * 内嵌商品推荐列表(仅assistant消息有此字段)
     */
    private java.util.List<ProductCard> products;

    @Data
    public static class ProductCard implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private BigDecimal score;
        private String reason;
        private Boolean tryOnSupported;
    }
}
