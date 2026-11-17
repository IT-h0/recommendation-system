import request from './index'

// 添加商品到购物车
export const addToCart = (data) => {
  return request({
    url: '/cart',
    method: 'post',
    data
  })
}

// 获取购物车
export const getCart = () => {
  return request({
    url: '/cart',
    method: 'get'
  })
}

// 删除购物车项
export const deleteCartItem = (id) => {
  return request({
    url: `/cart/${id}`,
    method: 'delete'
  })
}

// 清空购物车
export const clearCart = () => {
  return request({
    url: '/cart/clear',
    method: 'delete'
  })
}
