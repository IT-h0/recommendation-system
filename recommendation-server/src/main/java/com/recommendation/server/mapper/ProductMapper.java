package com.recommendation.server.mapper;

import com.recommendation.pojo.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品Mapper
 */
@Mapper
public interface ProductMapper {

    /**
     * 根据ID查询商品
     */
    Product selectById(@Param("id") Long id);

    /**
     * 查询所有商品
     */
    List<Product> selectAll();

    /**
     * 根据分类查询商品
     */
    List<Product> selectByCategory(@Param("categoryId") Long categoryId);

    /**
     * 插入商品
     */
    int insert(Product product);

    /**
     * 更新商品
     */
    int update(Product product);

    /**
     * 原子扣减库存并累加销量（WHERE stock >= qty 防止超卖）
     */
    int deductStock(@Param("id") Long id, @Param("qty") Integer qty);

    /**
     * 恢复库存并回退销量（取消订单时调用）
     */
    int restoreStock(@Param("id") Long id, @Param("qty") Integer qty);

    /**
     * 批量按ID查询商品（避免逐条查询的 N+1 问题）
     */
    List<Product> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 关键字搜索商品
     */
    List<Product> searchByKeyword(@Param("keyword") String keyword);
}
