package com.recommendation.server.service;

import com.recommendation.pojo.vo.ProductVO;

import java.util.List;

public interface ProductService {

    ProductVO getProductById(Long productId);

    List<ProductVO> getAllProducts();

    List<ProductVO> getProductsByCategory(Long categoryId);

    List<ProductVO> searchByKeyword(String keyword);

    List<ProductVO> getProductsByIds(List<Long> ids);

    void evictProductCache(Long productId, Long categoryId);
}
