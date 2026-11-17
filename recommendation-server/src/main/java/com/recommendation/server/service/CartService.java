package com.recommendation.server.service;

import com.recommendation.pojo.dto.AddCartItemDTO;
import com.recommendation.pojo.vo.CartVO;

import java.util.List;

public interface CartService {

    void addToCart(Long userId, AddCartItemDTO cartItemDTO);

    List<CartVO> getCartByUserId(Long userId);

    void deleteCartItem(Long cartItemId, Long userId);

    void clearCart(Long userId);
}
