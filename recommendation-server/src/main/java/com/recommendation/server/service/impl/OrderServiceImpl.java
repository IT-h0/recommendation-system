package com.recommendation.server.service.impl;

import com.recommendation.common.exception.BusinessException;
import com.recommendation.pojo.dto.CreateOrderDTO;
import com.recommendation.pojo.entity.CartItem;
import com.recommendation.pojo.entity.Order;
import com.recommendation.pojo.entity.OrderItem;
import com.recommendation.pojo.entity.Product;
import com.recommendation.pojo.vo.OrderItemVO;
import com.recommendation.pojo.vo.OrderVO;
import com.recommendation.server.mapper.CartMapper;
import com.recommendation.server.mapper.OrderMapper;
import com.recommendation.server.mapper.ProductMapper;
import com.recommendation.server.service.OrderService;
import com.recommendation.server.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final ProductService productService;

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, CreateOrderDTO createOrderDTO) {
        List<CartItem> cartItems = cartMapper.selectByUserId(userId);

        if (createOrderDTO.getCartItemIds() != null && !createOrderDTO.getCartItemIds().isEmpty()) {
            cartItems = cartItems.stream()
                    .filter(item -> createOrderDTO.getCartItemIds().contains(item.getId()))
                    .collect(Collectors.toList());
        }

        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车为空，无法创建订单");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = productMapper.selectById(cartItem.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在");
            }
            if (product.getStock() < cartItem.getQuantity()) {
                throw new BusinessException("商品【" + product.getName() + "】库存不足");
            }
            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0);
        order.setReceiverName(createOrderDTO.getReceiverName());
        order.setReceiverPhone(createOrderDTO.getReceiverPhone());
        order.setReceiverAddress(createOrderDTO.getReceiverAddress());
        order.setRemark(createOrderDTO.getRemark());
        order.setCreatedTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());

        orderMapper.insert(order);

        for (CartItem cartItem : cartItems) {
            Product product = productMapper.selectById(cartItem.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getImageUrl());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalPrice(product.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));

            orderMapper.insertOrderItem(orderItem);

            // 原子扣减，受影响行数为 0 说明库存不足，回滚整单
            if (productMapper.deductStock(product.getId(), cartItem.getQuantity()) != 1) {
                throw new BusinessException("商品【" + product.getName() + "】库存不足");
            }
            productService.evictProductCache(product.getId(), product.getCategoryId());
        }

        for (CartItem cartItem : cartItems) {
            cartMapper.delete(cartItem.getId());
        }

        return toOrderVO(order);
    }

    @Override
    public OrderVO getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该订单");
        }
        return toOrderVO(order);
    }

    @Override
    public List<OrderVO> getUserOrders(Long userId) {
        List<Order> orders = orderMapper.selectByUserId(userId);
        return orders.stream()
                .map(this::toOrderVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void payOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不正确");
        }
        orderMapper.updateStatus(orderId, 1);
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("只能取消待支付的订单");
        }

        List<OrderItem> items = orderMapper.selectOrderItemsByOrderId(orderId);
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                productMapper.restoreStock(product.getId(), item.getQuantity());
                productService.evictProductCache(product.getId(), product.getCategoryId());
            }
        }

        orderMapper.updateStatus(orderId, 4);
    }

    @Override
    @Transactional
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != 4) {
            throw new BusinessException("只能删除已取消的订单");
        }
        orderMapper.delete(orderId);
    }

    private String generateOrderNo() {
        // 毫秒级时间戳 + 6 位随机数，降低并发下单时的唯一索引冲突概率
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String random = String.format("%06d", (int) (Math.random() * 1000000));
        return "ORD" + timestamp + random;
    }

    private OrderVO toOrderVO(Order order) {
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setStatusDesc(getStatusDesc(order.getStatus()));

        List<OrderItem> items = orderMapper.selectOrderItemsByOrderId(order.getId());
        List<OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).collect(Collectors.toList());
        orderVO.setItems(itemVOs);

        return orderVO;
    }

    private String getStatusDesc(Integer status) {
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "已发货";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "未知";
        }
    }
}
