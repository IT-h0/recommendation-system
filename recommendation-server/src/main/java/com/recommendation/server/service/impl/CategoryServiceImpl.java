package com.recommendation.server.service.impl;

import com.recommendation.common.util.RedisUtil;
import com.recommendation.pojo.entity.Category;
import com.recommendation.pojo.vo.CategoryVO;
import com.recommendation.server.mapper.CategoryMapper;
import com.recommendation.server.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final RedisUtil redisUtil;

    private static final String CACHE_CATEGORY_ALL = "rec:category:all";
    private static final String CACHE_CATEGORY_TREE = "rec:category:tree";
    private static final long TTL_MINUTES = 30;

    @Override
    public List<CategoryVO> getAllCategories() {
        Object cached = redisUtil.get(CACHE_CATEGORY_ALL);
        if (cached != null) {
            return cast(cached);
        }
        List<Category> all = categoryMapper.selectAll();
        List<CategoryVO> vos = all.stream().map(this::toCategoryVO).collect(Collectors.toList());
        redisUtil.set(CACHE_CATEGORY_ALL, vos, TTL_MINUTES, TimeUnit.MINUTES);
        return vos;
    }

    @Override
    public List<CategoryVO> getCategoryTree() {
        Object cached = redisUtil.get(CACHE_CATEGORY_TREE);
        if (cached != null) {
            return cast(cached);
        }
        // 深拷贝一份再组装树，避免就地修改 getAllCategories() 的缓存对象，
        // 否则并发或缓存过期边界下会重复追加子分类
        List<CategoryVO> vos = getAllCategories().stream().map(v -> {
            CategoryVO copy = new CategoryVO();
            BeanUtils.copyProperties(v, copy);
            copy.setChildren(null);
            return copy;
        }).collect(Collectors.toList());
        Map<Long, CategoryVO> idMap = vos.stream()
                .collect(Collectors.toMap(CategoryVO::getId, v -> v));

        List<CategoryVO> roots = new ArrayList<>();
        for (CategoryVO vo : vos) {
            if (vo.getLevel() == 1) {
                roots.add(vo);
            } else if (vo.getParentId() != null) {
                CategoryVO parent = idMap.get(vo.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                }
            }
        }
        redisUtil.set(CACHE_CATEGORY_TREE, roots, TTL_MINUTES, TimeUnit.MINUTES);
        return roots;
    }

    @Override
    public void evictCategoryCache() {
        redisUtil.delete(CACHE_CATEGORY_ALL);
        redisUtil.delete(CACHE_CATEGORY_TREE);
    }

    private CategoryVO toCategoryVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }

    @SuppressWarnings("unchecked")
    private List<CategoryVO> cast(Object obj) {
        return (List<CategoryVO>) obj;
    }
}
