<template>
  <div class="product-detail" v-loading="loading">
    <el-card v-if="product">
      <el-row :gutter="30">
        <el-col :span="12">
          <img :src="product.imageUrl || 'https://via.placeholder.com/600x600'" class="product-image" />
        </el-col>
        <el-col :span="12">
          <h1 class="product-name">{{ product.name }}</h1>
          <p class="price">¥{{ product.price }}</p>
          <div class="product-meta">
            <p>销量: {{ product.sales }}</p>
            <p>库存: {{ product.stock }}</p>
          </div>
          <div class="quantity-selector">
            <span>数量:</span>
            <el-input-number v-model="quantity" :min="1" :max="product.stock" />
          </div>
          <div class="actions">
            <el-button type="primary" size="large" @click="handleAddToCart" :disabled="product.stock === 0">
              加入购物车
            </el-button>
          </div>
        </el-col>
      </el-row>

      <el-divider />

      <div class="description">
        <h3>商品详情</h3>
        <p>{{ product.description || '暂无商品描述' }}</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductById } from '@/api/product'
import { addToCart } from '@/api/cart'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const product = ref(null)
const quantity = ref(1)

const fetchProduct = async () => {
  try {
    loading.value = true
    const id = route.params.id
    product.value = await getProductById(id)
  } catch (error) {
    console.error('获取商品详情失败:', error)
    ElMessage.error('获取商品详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const handleAddToCart = async () => {
  if (!product.value || product.value.stock === 0) {
    ElMessage.warning('商品已售罄')
    return
  }

  try {
    await addToCart({
      productId: product.value.id,
      quantity: quantity.value
    })
    ElMessage.success('已添加到购物车')
  } catch (error) {
    console.error('添加购物车失败:', error)
    ElMessage.error('添加购物车失败')
  }
}

onMounted(() => {
  fetchProduct()
})
</script>

<style scoped>
.product-detail {
  max-width: 1200px;
  margin: 0 auto;
}

.product-image {
  width: 100%;
  height: auto;
  border-radius: 8px;
}

.product-name {
  font-size: 28px;
  margin-bottom: 20px;
}

.price {
  font-size: 32px;
  color: #f56c6c;
  font-weight: bold;
  margin: 20px 0;
}

.product-meta {
  margin: 20px 0;
  color: #606266;
}

.product-meta p {
  margin: 8px 0;
}

.quantity-selector {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 30px 0;
}

.actions {
  margin-top: 30px;
}

.description {
  margin-top: 30px;
}

.description h3 {
  margin-bottom: 15px;
}

.description p {
  line-height: 1.8;
  color: #606266;
}
</style>
