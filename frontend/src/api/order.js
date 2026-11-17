import request from './index'

// 创建订单
export const createOrder = (data) => {
  return request({
    url: '/orders',
    method: 'post',
    data
  })
}

// 获取订单详情
export const getOrderDetail = (id) => {
  return request({
    url: `/orders/${id}`,
    method: 'get'
  })
}

// 获取用户订单列表
export const getUserOrders = () => {
  return request({
    url: '/orders',
    method: 'get'
  })
}

// 支付订单
export const payOrder = (id) => {
  return request({
    url: `/orders/${id}/pay`,
    method: 'post'
  })
}

// 取消订单
export const cancelOrder = (id) => {
  return request({
    url: `/orders/${id}/cancel`,
    method: 'post'
  })
}

// 删除订单
export const deleteOrder = (id) => {
  return request({
    url: `/orders/${id}`,
    method: 'delete'
  })
}
