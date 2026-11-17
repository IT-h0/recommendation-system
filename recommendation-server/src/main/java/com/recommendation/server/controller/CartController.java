package com.recommendation.server.controller;

import com.recommendation.common.result.Result;
import com.recommendation.pojo.dto.AddCartItemDTO;
import com.recommendation.pojo.vo.CartVO;
import com.recommendation.server.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车Controller
 */
@Tag(name = "购物车管理", description = "购物车操作接口")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 添加商品到购物车
     */
    @Operation(summary = "添加商品到购物车")
    @PostMapping
    public Result<Void> addToCart(@RequestAttribute("userId") Long userId,
                                   @Valid @RequestBody AddCartItemDTO cartItemDTO) {
        cartService.addToCart(userId, cartItemDTO);
        return Result.success();
    }

    /**
     * 查询用户购物车
     */
    @Operation(summary = "查询用户购物车")
    @GetMapping
    public Result<List<CartVO>> getCart(@RequestAttribute("userId") Long userId) {
        List<CartVO> cartItems = cartService.getCartByUserId(userId);
        return Result.success(cartItems);
    }

    /**
     * 删除购物车项
     */
    @Operation(summary = "删除购物车项")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCartItem(@RequestAttribute("userId") Long userId,
                                       @PathVariable Long id) {
        cartService.deleteCartItem(id, userId);
        return Result.success();
    }

    /**
     * 清空购物车
     */
    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public Result<Void> clearCart(@RequestAttribute("userId") Long userId) {
        cartService.clearCart(userId);
        return Result.success();
    }
}
