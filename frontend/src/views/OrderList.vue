<template>
  <div class="order-list-page">
    <div class="order-list-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2>我的订单</h2>
        <div class="filter-tabs">
          <span
            v-for="tab in tabs"
            :key="tab.value"
            class="tab-item"
            :class="{ active: activeTab === tab.value }"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
            <el-badge v-if="tab.count > 0" :value="tab.count" class="tab-badge" />
          </span>
        </div>
      </div>

      <!-- 订单列表 -->
      <div class="order-list" v-loading="loading">
        <el-empty v-if="filteredOrders.length === 0" description="暂无订单" />
        
        <div v-else class="order-items">
          <div v-for="order in filteredOrders" :key="order.id" class="order-card">
            <!-- 订单头部 -->
            <div class="order-header">
              <div class="order-info">
                <span class="order-no">订单号：{{ order.orderNo }}</span>
                <span class="order-time">{{ formatTime(order.createdTime) }}</span>
              </div>
              <div class="order-status" :class="`status-${order.status}`">
                {{ order.statusDesc }}
              </div>
            </div>

            <!-- 订单商品 -->
            <div class="order-goods">
              <div
                v-for="item in order.items"
                :key="item.id"
                class="goods-item"
                @click="goToProduct(item.productId)"
              >
                <img :src="item.productImage || 'https://via.placeholder.com/80x80'" class="goods-image" />
                <div class="goods-info">
                  <div class="goods-name">{{ item.productName }}</div>
                  <div class="goods-meta">
                    <span class="goods-price">¥{{ item.price }}</span>
                    <span class="goods-quantity">x{{ item.quantity }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 订单底部 -->
            <div class="order-footer">
              <div class="order-total">
                <span class="total-label">共 {{ order.items.length }} 件商品</span>
                <span class="total-amount">
                  实付：<strong>¥{{ order.totalAmount.toFixed(2) }}</strong>
                </span>
              </div>
              <div class="order-actions">
                <el-button v-if="order.status === 0" type="primary" @click="handlePay(order)">
                  立即支付
                </el-button>
                <el-button v-if="order.status === 0" @click="handleCancel(order)">
                  取消订单
                </el-button>
                <el-button @click="goToDetail(order.id)">
                  查看详情
                </el-button>
                <el-button v-if="order.status === 4" type="danger" @click="handleDelete(order)">
                  删除订单
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserOrders, payOrder, cancelOrder, deleteOrder } from '@/api/order'

const router = useRouter()
const loading = ref(false)
const orders = ref([])
const activeTab = ref('all')

const tabs = [
  { label: '全部订单', value: 'all' },
  { label: '待支付', value: 'pending', count: computed(() => orders.value.filter(o => o.status === 0).length) },
  { label: '待发货', value: 'paid', count: computed(() => orders.value.filter(o => o.status === 1).length) },
  { label: '待收货', value: 'shipped', count: computed(() => orders.value.filter(o => o.status === 2).length) },
  { label: '已完成', value: 'completed', count: computed(() => orders.value.filter(o => o.status === 3).length) }
]

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orders.value
  const statusMap = {
    'pending': 0,
    'paid': 1,
    'shipped': 2,
    'completed': 3
  }
  return orders.value.filter(order => order.status === statusMap[activeTab.value])
})

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const fetchOrders = async () => {
  try {
    loading.value = true
    const res = await getUserOrders()
    orders.value = res || []
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const handlePay = async (order) => {
  try {
    await ElMessageBox.confirm('确认支付该订单?', '支付确认', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消',
      type: 'info'
    })
    await payOrder(order.id)
    ElMessage.success('支付成功')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('支付失败:', error)
      ElMessage.error(error.message || '支付失败')
    }
  }
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
      ElMessage.error(error.message || '取消订单失败')
    }
  }
}

const handleDelete = async (order) => {
  try {
    await ElMessageBox.confirm('确定要删除该订单吗?删除后不可恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'danger'
    })
    await deleteOrder(order.id)
    ElMessage.success('订单已删除')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除订单失败:', error)
      ElMessage.error(error.message || '删除订单失败')
    }
  }
}

const goToDetail = (orderId) => {
  router.push(`/order/${orderId}`)
}

const goToProduct = (productId) => {
  router.push(`/product/${productId}`)
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.order-list-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20px 0;
}

.order-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 20px 0;
  font-size: 24px;
  color: #333;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  border-bottom: 1px solid #eee;
  padding-bottom: 16px;
}

.tab-item {
  padding: 8px 16px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.tab-item:hover {
  color: #ff5000;
  background: #fff5f0;
}

.tab-item.active {
  color: #ff5000;
  background: #fff5f0;
  font-weight: 600;
}

.tab-badge :deep(.el-badge__content) {
  background-color: #ff5000;
}

.order-list {
  min-height: 400px;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fafafa;
  border-bottom: 1px solid #eee;
}

.order-info {
  display: flex;
  gap: 20px;
  align-items: center;
}

.order-no {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.order-time {
  font-size: 13px;
  color: #999;
}

.order-status {
  font-size: 14px;
  font-weight: 600;
}

.status-0 { color: #ff5000; }
.status-1 { color: #67c23a; }
.status-2 { color: #409eff; }
.status-3 { color: #909399; }
.status-4 { color: #999; }

.order-goods {
  padding: 16px 20px;
}

.goods-item {
  display: flex;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.2s;
}

.goods-item:last-child {
  border-bottom: none;
}

.goods-item:hover {
  background: #fafafa;
}

.goods-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #eee;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.goods-name {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
}

.goods-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.goods-price {
  font-size: 14px;
  color: #ff5000;
  font-weight: 600;
}

.goods-quantity {
  font-size: 13px;
  color: #999;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-top: 1px solid #eee;
  background: #fafafa;
}

.order-total {
  display: flex;
  align-items: center;
  gap: 16px;
}

.total-label {
  font-size: 13px;
  color: #999;
}

.total-amount {
  font-size: 14px;
  color: #666;
}

.total-amount strong {
  font-size: 18px;
  color: #ff5000;
  font-weight: 700;
}

.order-actions {
  display: flex;
  gap: 12px;
}

.order-actions .el-button {
  min-width: 80px;
}
</style>
