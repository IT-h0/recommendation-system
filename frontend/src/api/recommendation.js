import request from './index'

// 获取推荐商品
export const getRecommendations = (topN = 10) => {
  return request({
    url: '/recommendation',
    method: 'get',
    params: { topN }
  })
}
