<template>
  <div class="cart">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>购物车</h2>
          <el-button type="danger" size="small" @click="handleClearCart" :disabled="cartItems.length === 0">
            清空购物车
          </el-button>
        </div>
      </template>

      <div v-loading="loading">
        <el-empty v-if="!loading && cartItems.length === 0" description="购物车是空的" />

        <div v-else>
          <el-table :data="cartItems" style="width: 100%">
            <el-table-column label="商品信息" width="400">
              <template #default="{ row }">
                <div class="product-info">
                  <img :src="row.productImage || 'https://via.placeholder.com/80x80'" class="product-image" />
                  <div class="product-name">{{ row.productName }}</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="price" label="单价" width="150">
              <template #default="{ row }">
                <span class="price">¥{{ row.price }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="quantity" label="数量" width="200">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="1" @change="handleQuantityChange(row)" />
              </template>
            </el-table-column>

            <el-table-column prop="totalPrice" label="小计" width="150">
              <template #default="{ row }">
                <span class="total-price">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button type="danger" size="small" @click="handleDelete(row.id)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="cart-footer">
            <div class="total">
              <span>商品总数: {{ totalQuantity }} 件</span>
              <span class="total-amount">总计: ¥{{ totalAmount.toFixed(2) }}</span>
            </div>
            <el-button type="primary" size="large" @click="handleCheckout">
              去结算
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCart, deleteCartItem, clearCart } from '@/api/cart'

const loading = ref(true)
const cartItems = ref([])

const totalQuantity = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.quantity, 0)
})

const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const fetchCart = async () => {
  try {
    loading.value = true
    cartItems.value = await getCart()
  } catch (error) {
    console.error('获取购物车失败:', error)
    ElMessage.error('获取购物车失败')
  } finally {
    loading.value = false
  }
}

const handleQuantityChange = (row) => {
  // 更新总价
  row.totalPrice = (row.price * row.quantity).toFixed(2)
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteCartItem(id)
    ElMessage.success('删除成功')
    fetchCart()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleClearCart = async () => {
  try {
    await ElMessageBox.confirm('确定要清空购物车吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await clearCart()
    ElMessage.success('购物车已清空')
    fetchCart()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空购物车失败:', error)
      ElMessage.error('清空购物车失败')
    }
  }
}

const handleCheckout = () => {
  if (cartItems.value.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }
  // 跳转到订单确认页面
  router.push('/checkout')
}

onMounted(() => {
  fetchCart()
})
</script>

<style scoped>
.cart {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.product-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.product-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price, .total-price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.total {
  display: flex;
  gap: 20px;
  font-size: 16px;
}

.total-amount {
  font-size: 20px;
  color: #f56c6c;
  font-weight: bold;
}
</style>
