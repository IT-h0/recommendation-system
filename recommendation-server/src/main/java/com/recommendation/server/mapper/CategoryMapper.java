package com.recommendation.server.mapper;

import com.recommendation.pojo.entity.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 分类Mapper
 */
@Mapper
public interface CategoryMapper {

    /**
     * 查询所有分类
     */
    List<Category> selectAll();

    /**
     * 根据父分类ID查询子分类
     */
    List<Category> selectByParentId(Long parentId);

    /**
     * 根据层级查询分类
     */
    List<Category> selectByLevel(Integer level);
}
