<template>
  <div class="product-card-inline" @click="goToDetail">
    <img
      :src="product.productImage || defaultImg"
      class="product-img"
      @error="(e) => (e.target.src = defaultImg)"
    />
    <div class="product-info">
      <div class="product-name">{{ product.productName }}</div>
      <div class="product-price">¥{{ product.price }}</div>
      <div v-if="product.reason" class="product-reason">{{ product.reason }}</div>
    </div>
    <div class="product-actions" @click.stop>
      <el-button size="small" type="primary" @click="goToDetail">查看详情</el-button>
      <el-button
        v-if="product.tryOnSupported"
        size="small"
        type="warning"
        @click="$emit('tryOn', product)"
      >试戴看看</el-button>
      <el-button size="small" @click="addCart">加入购物车</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addToCart } from '@/api/cart'

const props = defineProps({
  product: { type: Object, required: true }
})

defineEmits(['tryOn'])

const router = useRouter()
const defaultImg = 'https://via.placeholder.com/120x120?text=No+Image'

function goToDetail() {
  router.push(`/product/${props.product.productId}`)
}

async function addCart() {
  try {
    await addToCart({ productId: props.product.productId, quantity: 1 })
    ElMessage.success('已加入购物车')
  } catch (e) {
    ElMessage.error('添加失败')
  }
}
</script>

<style scoped>
.product-card-inline {
  display: flex;
  gap: 12px;
  padding: 10px;
  margin: 8px 0;
  background: #fff;
  border: 1px solid #ffe4d6;
  border-radius: 10px;
  cursor: pointer;
  transition: box-shadow 0.2s;
  align-items: center;
}
.product-card-inline:hover {
  box-shadow: 0 2px 12px rgba(255, 80, 0, 0.1);
}

.product-img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.product-info {
  flex: 1;
  min-width: 0;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.product-price {
  color: #ff5000;
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 4px;
}

.product-reason {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
</style>
