<template>
  <div class="checkout-page">
    <div class="checkout-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2>确认订单</h2>
        <el-steps :active="1" simple>
          <el-step title="购物车" />
          <el-step title="确认订单" />
          <el-step title="支付" />
          <el-step title="完成" />
        </el-steps>
      </div>

      <!-- 收货地址 -->
      <div class="section">
        <h3 class="section-title">
          <el-icon><Location /></el-icon>
          收货地址
        </h3>
        <el-form :model="orderForm" :rules="rules" ref="orderFormRef" label-position="top">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="收货人姓名" prop="receiverName">
                <el-input v-model="orderForm.receiverName" placeholder="请输入收货人姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="联系电话" prop="receiverPhone">
                <el-input v-model="orderForm.receiverPhone" placeholder="请输入联系电话" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="详细地址" prop="receiverAddress">
                <el-input v-model="orderForm.receiverAddress" placeholder="请输入详细地址" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="订单备注">
            <el-input
              v-model="orderForm.remark"
              type="textarea"
              :rows="2"
              placeholder="请输入订单备注（选填）"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 商品列表 -->
      <div class="section">
        <h3 class="section-title">
          <el-icon><ShoppingBag /></el-icon>
          商品清单
        </h3>
        <el-table :data="cartItems" style="width: 100%" v-loading="loading">
          <el-table-column label="商品信息" min-width="350">
            <template #default="{ row }">
              <div class="product-info">
                <img :src="row.productImage || 'https://via.placeholder.com/80x80'" class="product-image" />
                <div class="product-detail">
                  <div class="product-name">{{ row.productName }}</div>
                  <div class="product-price">¥{{ row.price }}</div>
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
              <span class="subtotal">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 订单汇总 -->
      <div class="section order-summary">
        <div class="summary-row">
          <span>商品总数：</span>
          <span class="value">{{ totalQuantity }} 件</span>
        </div>
        <div class="summary-row">
          <span>商品总额：</span>
          <span class="value">¥{{ totalAmount.toFixed(2) }}</span>
        </div>
        <div class="summary-row">
          <span>运费：</span>
          <span class="value">¥0.00</span>
        </div>
        <div class="summary-row total">
          <span>应付总额：</span>
          <span class="total-amount">¥{{ totalAmount.toFixed(2) }}</span>
        </div>
      </div>

      <!-- 提交按钮 -->
      <div class="submit-bar">
        <div class="submit-info">
          <span class="label">应付总额：</span>
          <span class="amount">¥{{ totalAmount.toFixed(2) }}</span>
        </div>
        <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
          提交订单
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, ShoppingBag } from '@element-plus/icons-vue'
import { getCart } from '@/api/cart'
import { createOrder } from '@/api/order'

const router = useRouter()
const route = useRoute()
const orderFormRef = ref(null)

const loading = ref(false)
const submitting = ref(false)
const cartItems = ref([])

const orderForm = ref({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  remark: '',
  cartItemIds: []
})

const rules = {
  receiverName: [
    { required: true, message: '请输入收货人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  receiverPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  receiverAddress: [
    { required: true, message: '请输入详细地址', trigger: 'blur' },
    { min: 5, max: 200, message: '长度在 5 到 200 个字符', trigger: 'blur' }
  ]
}

const totalQuantity = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.quantity, 0)
})

const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const fetchCart = async () => {
  try {
    loading.value = true
    const res = await getCart()
    // 如果URL中有指定购物车项ID，则只选择这些项
    const selectedIds = route.query.cartItemIds
    if (selectedIds) {
      const ids = selectedIds.split(',').map(Number)
      cartItems.value = (res || []).filter(item => ids.includes(item.id))
    } else {
      cartItems.value = res || []
    }
    
    if (cartItems.value.length === 0) {
      ElMessage.warning('购物车为空，无法结算')
      router.push('/cart')
    }
  } catch (error) {
    console.error('获取购物车失败:', error)
    ElMessage.error('获取购物车失败')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  const valid = await orderFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    submitting.value = true
    const data = {
      ...orderForm.value,
      cartItemIds: cartItems.value.map(item => item.id)
    }
    const res = await createOrder(data)
    ElMessage.success('订单创建成功')
    // 跳转到支付页面或订单详情页
    router.push(`/order/${res.id}`)
  } catch (error) {
    console.error('创建订单失败:', error)
    ElMessage.error(error.message || '创建订单失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchCart()
})
</script>

<style scoped>
.checkout-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20px 0;
}

.checkout-container {
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

.section {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 20px 0;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.section-title .el-icon {
  color: #ff5000;
  font-size: 20px;
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
  margin-bottom: 8px;
}

.product-price {
  font-size: 13px;
  color: #999;
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

.order-summary {
  text-align: right;
}

.summary-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #666;
}

.summary-row .value {
  color: #333;
  min-width: 100px;
}

.summary-row.total {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eee;
  font-size: 16px;
}

.summary-row.total .total-amount {
  color: #ff5000;
  font-size: 24px;
  font-weight: 700;
  min-width: 100px;
}

.submit-bar {
  position: sticky;
  bottom: 0;
  background: #fff;
  padding: 16px 24px;
  border-radius: 8px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 24px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

.submit-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.submit-info .label {
  font-size: 14px;
  color: #666;
}

.submit-info .amount {
  font-size: 24px;
  color: #ff5000;
  font-weight: 700;
}

.submit-bar .el-button {
  padding: 12px 40px;
  font-size: 16px;
}
</style>
