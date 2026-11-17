package com.recommendation.server.service.impl;

import com.recommendation.common.exception.BusinessException;
import com.recommendation.common.util.RedisUtil;
import com.recommendation.pojo.entity.Category;
import com.recommendation.pojo.entity.Product;
import com.recommendation.pojo.vo.ProductVO;
import com.recommendation.server.mapper.CategoryMapper;
import com.recommendation.server.mapper.ProductMapper;
import com.recommendation.server.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final RedisUtil redisUtil;

    private static final String CACHE_CATEGORY_NAME_MAP = "rec:category:nameMap";
    private static final String CACHE_PRODUCT_ALL = "rec:product:all";
    private static final String CACHE_PRODUCT_ID = "rec:product:id:";
    private static final String CACHE_PRODUCT_CATEGORY = "rec:product:category:";
    private static final String CACHE_PRODUCT_SEARCH = "rec:product:search:";

    private static final long TTL_CATEGORY_MAP = 30;
    private static final long TTL_PRODUCT_LIST = 5;
    private static final long TTL_PRODUCT_DETAIL = 10;
    private static final long TTL_SEARCH = 2;

    @Override
    public ProductVO getProductById(Long productId) {
        String key = CACHE_PRODUCT_ID + productId;
        Object cached = redisUtil.get(key);
        if (cached != null) {
            return (ProductVO) cached;
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        ProductVO vo = toProductVO(product, buildCategoryNameMap());
        redisUtil.set(key, vo, TTL_PRODUCT_DETAIL, TimeUnit.MINUTES);
        return vo;
    }

    @Override
    public List<ProductVO> getAllProducts() {
        Object cached = redisUtil.get(CACHE_PRODUCT_ALL);
        if (cached != null) {
            return cast(cached);
        }
        List<Product> products = productMapper.selectAll();
        Map<String, String> nameMap = buildCategoryNameMap();
        List<ProductVO> vos = products.stream()
                .map(p -> toProductVO(p, nameMap))
                .collect(Collectors.toList());
        redisUtil.set(CACHE_PRODUCT_ALL, vos, TTL_PRODUCT_LIST, TimeUnit.MINUTES);
        return vos;
    }

    @Override
    public List<ProductVO> getProductsByCategory(Long categoryId) {
        String key = CACHE_PRODUCT_CATEGORY + categoryId;
        Object cached = redisUtil.get(key);
        if (cached != null) {
            return cast(cached);
        }
        List<Product> products = productMapper.selectByCategory(categoryId);
        Map<String, String> nameMap = buildCategoryNameMap();
        List<ProductVO> vos = products.stream()
                .map(p -> toProductVO(p, nameMap))
                .collect(Collectors.toList());
        redisUtil.set(key, vos, TTL_PRODUCT_LIST, TimeUnit.MINUTES);
        return vos;
    }

    @Override
    public List<ProductVO> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String key = CACHE_PRODUCT_SEARCH + keyword.trim();
        Object cached = redisUtil.get(key);
        if (cached != null) {
            return cast(cached);
        }
        List<Product> products = productMapper.searchByKeyword(keyword.trim());
        Map<String, String> nameMap = buildCategoryNameMap();
        List<ProductVO> vos = products.stream()
                .map(p -> toProductVO(p, nameMap))
                .collect(Collectors.toList());
        redisUtil.set(key, vos, TTL_SEARCH, TimeUnit.MINUTES);
        return vos;
    }

    @Override
    public List<ProductVO> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<String, String> nameMap = buildCategoryNameMap();
        return productMapper.selectByIds(ids).stream()
                .map(p -> toProductVO(p, nameMap))
                .collect(Collectors.toList());
    }

    @Override
    public void evictProductCache(Long productId, Long categoryId) {
        redisUtil.delete(CACHE_PRODUCT_ALL);
        if (productId != null) {
            redisUtil.delete(CACHE_PRODUCT_ID + productId);
        }
        if (categoryId != null) {
            redisUtil.delete(CACHE_PRODUCT_CATEGORY + categoryId);
        }
    }

    private Map<String, String> buildCategoryNameMap() {
        Object cached = redisUtil.get(CACHE_CATEGORY_NAME_MAP);
        if (cached != null) {
            return castMap(cached);
        }
        List<Category> all = categoryMapper.selectAll();
        // 使用 String 作为 key：Redis 的 JSON 序列化只保留字符串键，
        // 若用 Long key，缓存命中后 get(Long) 永远取不到值，导致分类名显示为空。
        Map<String, String> nameMap = all.stream().collect(Collectors.toMap(
                c -> String.valueOf(c.getId()),
                Category::getName,
                (a, b) -> a
        ));
        redisUtil.set(CACHE_CATEGORY_NAME_MAP, nameMap, TTL_CATEGORY_MAP, TimeUnit.MINUTES);
        return nameMap;
    }

    private ProductVO toProductVO(Product product, Map<String, String> categoryNameMap) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        if (product.getCategoryId() != null) {
            vo.setCategoryName(categoryNameMap.getOrDefault(String.valueOf(product.getCategoryId()), ""));
        }
        return vo;
    }

    @SuppressWarnings("unchecked")
    private List<ProductVO> cast(Object obj) {
        return (List<ProductVO>) obj;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> castMap(Object obj) {
        return (Map<String, String>) obj;
    }
}
