<template>
  <div class="order-detail-page">
    <div class="order-detail-container">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-left">
          <el-button text @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <h2>订单详情</h2>
        </div>
        <el-steps :active="activeStep" simple class="header-steps">
          <el-step title="提交订单" />
          <el-step title="支付" />
          <el-step title="发货" />
          <el-step title="完成" />
        </el-steps>
      </div>

      <!-- 订单状态 -->
      <div class="status-section" :class="`status-${order.status}`">
        <div class="status-icon">
          <el-icon v-if="order.status === 0"><Clock /></el-icon>
          <el-icon v-else-if="order.status === 1"><Check /></el-icon>
          <el-icon v-else-if="order.status === 2"><Box /></el-icon>
          <el-icon v-else-if="order.status === 3"><CircleCheck /></el-icon>
          <el-icon v-else><CircleClose /></el-icon>
        </div>
        <div class="status-info">
          <h3>{{ order.statusDesc }}</h3>
          <p v-if="order.status === 0">请在30分钟内完成支付，超时订单将自动取消</p>
          <p v-else-if="order.status === 1">订单已支付，等待商家发货</p>
          <p v-else-if="order.status === 2">商品已发货，请注意查收</p>
          <p v-else-if="order.status === 3">订单已完成，感谢您的购买</p>
          <p v-else>订单已取消</p>
        </div>
        <div class="status-actions" v-if="order.status === 0">
          <el-button type="primary" size="large" @click="handlePay">立即支付</el-button>
          <el-button size="large" @click="handleCancel">取消订单</el-button>
        </div>
        <div class="status-actions" v-else-if="order.status === 4">
          <el-button type="danger" size="large" @click="handleDelete">删除订单</el-button>
        </div>
      </div>

      <!-- 订单信息 -->
      <div class="info-section">
        <el-row :gutter="40">
          <el-col :span="8">
            <h4>订单信息</h4>
            <div class="info-item">
              <span class="label">订单编号：</span>
              <span class="value">{{ order.orderNo }}</span>
            </div>
            <div class="info-item">
              <span class="label">创建时间：</span>
              <span class="value">{{ formatTime(order.createdTime) }}</span>
            </div>
            <div class="info-item" v-if="order.updatedTime">
              <span class="label">更新时间：</span>
              <span class="value">{{ formatTime(order.updatedTime) }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <h4>收货信息</h4>
            <div class="info-item">
              <span class="label">收货人：</span>
              <span class="value">{{ order.receiverName }}</span>
            </div>
            <div class="info-item">
              <span class="label">联系电话：</span>
              <span class="value">{{ order.receiverPhone }}</span>
            </div>
            <div class="info-item">
              <span class="label">收货地址：</span>
              <span class="value">{{ order.receiverAddress }}</span>
            </div>
          </el-col>
          <el-col :span="8" v-if="order.remark">
            <h4>订单备注</h4>
            <div class="info-item">
              <span class="value">{{ order.remark }}</span>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 商品列表 -->
      <div class="goods-section">
        <h3>商品信息</h3>
        <el-table :data="order.items" style="width: 100%">
          <el-table-column label="商品信息" min-width="400">
            <template #default="{ row }">
              <div class="product-info">
                <img :src="row.productImage || 'https://via.placeholder.com/80x80'" class="product-image" />
                <div class="product-detail">
                  <div class="product-name">{{ row.productName }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="150" align="center">
            <template #default="{ row }">
              <span class="price">¥{{ row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="120" align="center">
            <template #default="{ row }">
              <span class="quantity">x{{ row.quantity }}</span>
            </template>
          </el-table-column>
          <el-table-column label="小计" width="150" align="center">
            <template #default="{ row }">
              <span class="subtotal">¥{{ row.totalPrice.toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="order-total">
          <div class="total-row">
            <span>商品总额：</span>
            <span>¥{{ order.totalAmount.toFixed(2) }}</span>
          </div>
          <div class="total-row">
            <span>运费：</span>
            <span>¥0.00</span>
          </div>
          <div class="total-row final">
            <span>实付金额：</span>
            <span class="final-amount">¥{{ order.totalAmount.toFixed(2) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Clock, Check, Box, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getOrderDetail, payOrder, cancelOrder, deleteOrder } from '@/api/order'

const router = useRouter()
const route = useRoute()
const orderId = route.params.id

const order = ref({
  items: []
})
const loading = ref(false)

const activeStep = computed(() => {
  const stepMap = {
    0: 1,
    1: 2,
    2: 3,
    3: 4,
    4: 1
  }
  return stepMap[order.value.status] || 1
})

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const fetchOrderDetail = async () => {
  try {
    loading.value = true
    const res = await getOrderDetail(orderId)
    order.value = res
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
  } finally {
    loading.value = false
  }
}

const handlePay = async () => {
  try {
    await ElMessageBox.confirm('确认支付该订单?', '支付确认', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消',
      type: 'info'
    })
    await payOrder(orderId)
    ElMessage.success('支付成功')
    fetchOrderDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('支付失败:', error)
      ElMessage.error(error.message || '支付失败')
    }
  }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelOrder(orderId)
    ElMessage.success('订单已取消')
    fetchOrderDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
      ElMessage.error(error.message || '取消订单失败')
    }
  }
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除该订单吗?删除后不可恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'danger'
    })
    await deleteOrder(orderId)
    ElMessage.success('订单已删除')
    router.push('/orders')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除订单失败:', error)
      ElMessage.error(error.message || '删除订单失败')
    }
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchOrderDetail()
})
</script>

<style scoped>
.order-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20px 0;
}

.order-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header {
  background: #fff;
  padding: 20px 24px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.header-steps {
  width: 400px;
}

.status-section {
  background: #fff;
  padding: 32px 24px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.status-0 { background: linear-gradient(135deg, #fff5f0 0%, #fff 100%); border-left: 4px solid #ff5000; }
.status-1 { background: linear-gradient(135deg, #f0f9ff 0%, #fff 100%); border-left: 4px solid #409eff; }
.status-2 { background: linear-gradient(135deg, #f0fff4 0%, #fff 100%); border-left: 4px solid #67c23a; }
.status-3 { background: linear-gradient(135deg, #f6f6f6 0%, #fff 100%); border-left: 4px solid #909399; }
.status-4 { background: linear-gradient(135deg, #fef0f0 0%, #fff 100%); border-left: 4px solid #f56c6c; }

.status-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.status-0 .status-icon { background: #ff5000; color: #fff; }
.status-1 .status-icon { background: #409eff; color: #fff; }
.status-2 .status-icon { background: #67c23a; color: #fff; }
.status-3 .status-icon { background: #909399; color: #fff; }
.status-4 .status-icon { background: #f56c6c; color: #fff; }

.status-info {
  flex: 1;
}

.status-info h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #333;
}

.status-info p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.status-actions {
  display: flex;
  gap: 12px;
}

.info-section {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.info-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.info-item {
  margin-bottom: 12px;
  font-size: 14px;
  line-height: 1.5;
}

.info-item .label {
  color: #999;
}

.info-item .value {
  color: #333;
}

.goods-section {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}

.goods-section h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.product-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #eee;
}

.product-detail {
  flex: 1;
}

.product-name {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
}

.price {
  color: #666;
  font-size: 14px;
}

.quantity {
  color: #666;
  font-size: 14px;
}

.subtotal {
  color: #ff5000;
  font-weight: 600;
  font-size: 16px;
}

.order-total {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #eee;
  text-align: right;
}

.total-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #666;
}

.total-row.final {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #eee;
  font-size: 16px;
}

.final-amount {
  color: #ff5000;
  font-size: 28px;
  font-weight: 700;
}
</style>
