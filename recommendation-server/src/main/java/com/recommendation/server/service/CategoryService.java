package com.recommendation.server.service;

import com.recommendation.pojo.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    List<CategoryVO> getAllCategories();

    List<CategoryVO> getCategoryTree();

    void evictCategoryCache();
}
