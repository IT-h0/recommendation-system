package com.recommendation.server.mapper;

import com.recommendation.pojo.entity.Order;
import com.recommendation.pojo.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper {

    /**
     * 插入订单
     */
    int insert(Order order);

    /**
     * 插入订单详情
     */
    int insertOrderItem(OrderItem orderItem);

    /**
     * 根据ID查询订单
     */
    Order selectById(@Param("id") Long id);

    /**
     * 根据订单号查询订单
     */
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据用户ID查询订单列表
     */
    List<Order> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询订单详情列表
     */
    List<OrderItem> selectOrderItemsByOrderId(@Param("orderId") Long orderId);

    /**
     * 更新订单状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 删除订单
     */
    int delete(@Param("id") Long id);
}
