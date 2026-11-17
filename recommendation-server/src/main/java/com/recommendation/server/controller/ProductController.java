package com.recommendation.server.controller;

import com.recommendation.common.result.Result;
import com.recommendation.pojo.vo.ProductVO;
import com.recommendation.server.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品Controller
 */
@Tag(name = "商品管理", description = "商品查询接口")
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 根据ID查询商品
     */
    @Operation(summary = "根据ID查询商品")
    @GetMapping("/{id}")
    public Result<ProductVO> getProductById(@PathVariable Long id) {
        ProductVO productVO = productService.getProductById(id);
        return Result.success(productVO);
    }

    /**
     * 查询所有商品
     */
    @Operation(summary = "查询所有商品")
    @GetMapping("/list")
    public Result<List<ProductVO>> getAllProducts() {
        List<ProductVO> products = productService.getAllProducts();
        return Result.success(products);
    }

    /**
     * 根据分类查询商品
     */
    @Operation(summary = "根据分类查询商品")
    @GetMapping("/category/{categoryId}")
    public Result<List<ProductVO>> getProductsByCategory(@PathVariable Long categoryId) {
        List<ProductVO> products = productService.getProductsByCategory(categoryId);
        return Result.success(products);
    }

    /**
     * 关键字搜索商品
     */
    @Operation(summary = "关键字搜索商品")
    @GetMapping("/search")
    public Result<List<ProductVO>> searchProducts(@RequestParam String keyword) {
        List<ProductVO> products = productService.searchByKeyword(keyword);
        return Result.success(products);
    }
}
