package com.recommendation.server.service;

import com.recommendation.pojo.dto.CreateOrderDTO;
import com.recommendation.pojo.vo.OrderVO;

import java.util.List;

public interface OrderService {

    OrderVO createOrder(Long userId, CreateOrderDTO createOrderDTO);

    OrderVO getOrderDetail(Long userId, Long orderId);

    List<OrderVO> getUserOrders(Long userId);

    void payOrder(Long userId, Long orderId);

    void cancelOrder(Long userId, Long orderId);

    void deleteOrder(Long userId, Long orderId);
}
