package com.recommendation.server.service.impl;

import com.recommendation.common.exception.BusinessException;
import com.recommendation.pojo.dto.AddCartItemDTO;
import com.recommendation.pojo.entity.CartItem;
import com.recommendation.pojo.entity.Product;
import com.recommendation.pojo.vo.CartVO;
import com.recommendation.server.mapper.CartMapper;
import com.recommendation.server.mapper.ProductMapper;
import com.recommendation.server.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;

    @Transactional
    @Override
    public void addToCart(Long userId, AddCartItemDTO cartItemDTO) {
        Product product = productMapper.selectById(cartItemDTO.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        if (product.getStock() < cartItemDTO.getQuantity()) {
            throw new BusinessException("库存不足");
        }

        CartItem existCartItem = cartMapper.selectByUserAndProduct(userId, cartItemDTO.getProductId());

        if (existCartItem != null) {
            existCartItem.setQuantity(existCartItem.getQuantity() + cartItemDTO.getQuantity());
            cartMapper.update(existCartItem);
        } else {
            CartItem cartItem = new CartItem();
            BeanUtils.copyProperties(cartItemDTO, cartItem);
            cartItem.setUserId(userId);
            cartMapper.insert(cartItem);
        }
    }

    @Override
    public List<CartVO> getCartByUserId(Long userId) {
        List<CartItem> cartItems = cartMapper.selectByUserId(userId);
        return cartItems.stream()
                .map(this::toCartVO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCartItem(Long cartItemId, Long userId) {
        if (cartMapper.deleteByIdAndUserId(cartItemId, userId) != 1) {
            throw new BusinessException("购物车项不存在或无权操作");
        }
    }

    @Override
    public void clearCart(Long userId) {
        cartMapper.deleteByUserId(userId);
    }

    private CartVO toCartVO(CartItem cartItem) {
        CartVO cartVO = new CartVO();
        BeanUtils.copyProperties(cartItem, cartVO);

        Product product = productMapper.selectById(cartItem.getProductId());
        if (product != null) {
            cartVO.setProductName(product.getName());
            cartVO.setProductImage(product.getImageUrl());
            cartVO.setPrice(product.getPrice());
            cartVO.setTotalPrice(product.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
        }

        return cartVO;
    }
}
