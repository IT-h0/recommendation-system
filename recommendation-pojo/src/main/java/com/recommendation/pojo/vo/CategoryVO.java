package com.recommendation.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分类VO
 */
@Data
public class CategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 分类层级（1=一级，2=二级）
     */
    private Integer level;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 图标URL
     */
    private String icon;

    /**
     * 子分类列表（level=1时填充）
     */
    private List<CategoryVO> children;
}
