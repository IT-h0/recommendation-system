package com.recommendation.server.mapper;

import com.recommendation.pojo.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车Mapper
 */
@Mapper
public interface CartMapper {

    /**
     * 根据用户ID查询购物车
     */
    List<CartItem> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和商品ID查询购物车项
     */
    CartItem selectByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 插入购物车项
     */
    int insert(CartItem cartItem);

    /**
     * 更新购物车项
     */
    int update(CartItem cartItem);

    /**
     * 删除购物车项
     */
    int delete(@Param("id") Long id);

    /**
     * 按ID和用户删除购物车项（校验归属，防止越权删除）
     */
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 清空用户购物车
     */
    int deleteByUserId(@Param("userId") Long userId);
}
