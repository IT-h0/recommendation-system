import request from './index'

// 获取所有商品
export const getAllProducts = () => {
  return request({
    url: '/product/list',
    method: 'get'
  })
}

// 根据ID获取商品
export const getProductById = (id) => {
  return request({
    url: `/product/${id}`,
    method: 'get'
  })
}

// 根据分类获取商品
export const getProductsByCategory = (categoryId) => {
  return request({
    url: `/product/category/${categoryId}`,
    method: 'get'
  })
}

// 搜索商品
export const searchProducts = (keyword) => {
  return request({
    url: '/product/search',
    method: 'get',
    params: { keyword }
  })
}

// 获取所有分类（平铺，含 level/parentId）
export const getAllCategories = () => {
  return request({
    url: '/category/list',
    method: 'get'
  })
}

// 获取分类树（一级分类 + 二级子分类）
export const getCategoryTree = () => {
  return request({
    url: '/category/tree',
    method: 'get'
  })
}
