package com.recommendation.server.controller;

import com.recommendation.common.result.Result;
import com.recommendation.pojo.vo.CategoryVO;
import com.recommendation.server.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类Controller
 */
@Tag(name = "分类管理", description = "商品分类查询接口")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取所有分类（平铺，含 level/parentId）
     */
    @Operation(summary = "获取所有分类")
    @GetMapping("/list")
    public Result<List<CategoryVO>> getAllCategories() {
        return Result.success(categoryService.getAllCategories());
    }

    /**
     * 获取分类树（一级 + 二级子分类）
     */
    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public Result<List<CategoryVO>> getCategoryTree() {
        return Result.success(categoryService.getCategoryTree());
    }
}
