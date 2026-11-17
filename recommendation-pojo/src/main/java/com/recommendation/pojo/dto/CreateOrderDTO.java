package com.recommendation.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建订单DTO
 */
@Data
public class CreateOrderDTO {

    /**
     * 购物车项ID列表（从购物车结算时传入）
     */
    private List<Long> cartItemIds;

    /**
     * 收货人姓名
     */
    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    /**
     * 收货人电话
     */
    @NotBlank(message = "收货人电话不能为空")
    private String receiverPhone;

    /**
     * 收货地址
     */
    @NotBlank(message = "收货地址不能为空")
    private String receiverAddress;

    /**
     * 备注
     */
    private String remark;
}
