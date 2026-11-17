package com.recommendation.server.controller;

import com.recommendation.common.result.Result;
import com.recommendation.pojo.dto.CreateOrderDTO;
import com.recommendation.pojo.vo.OrderVO;
import com.recommendation.server.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单Controller
 */
@Tag(name = "订单管理", description = "订单操作接口")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     */
    @Operation(summary = "创建订单")
    @PostMapping
    public Result<OrderVO> createOrder(@RequestAttribute("userId") Long userId,
                                        @Valid @RequestBody CreateOrderDTO createOrderDTO) {
        OrderVO orderVO = orderService.createOrder(userId, createOrderDTO);
        return Result.success(orderVO);
    }

    /**
     * 获取订单详情
     */
    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> getOrderDetail(@RequestAttribute("userId") Long userId,
                                          @PathVariable Long id) {
        OrderVO orderVO = orderService.getOrderDetail(userId, id);
        return Result.success(orderVO);
    }

    /**
     * 获取用户订单列表
     */
    @Operation(summary = "获取用户订单列表")
    @GetMapping
    public Result<List<OrderVO>> getUserOrders(@RequestAttribute("userId") Long userId) {
        List<OrderVO> orders = orderService.getUserOrders(userId);
        return Result.success(orders);
    }

    /**
     * 支付订单
     */
    @Operation(summary = "支付订单")
    @PostMapping("/{id}/pay")
    public Result<Void> payOrder(@RequestAttribute("userId") Long userId,
                                 @PathVariable Long id) {
        orderService.payOrder(userId, id);
        return Result.success();
    }

    /**
     * 取消订单
     */
    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@RequestAttribute("userId") Long userId,
                                    @PathVariable Long id) {
        orderService.cancelOrder(userId, id);
        return Result.success();
    }

    /**
     * 删除订单
     */
    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@RequestAttribute("userId") Long userId,
                                    @PathVariable Long id) {
        orderService.deleteOrder(userId, id);
        return Result.success();
    }
}
