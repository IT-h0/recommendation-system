<template>
  <div class="products">
    <el-card>
      <template #header>
        <h2>商品列表</h2>
      </template>

      <div v-loading="loading" class="product-list">
        <el-empty v-if="!loading && products.length === 0" description="暂无商品数据" />

        <el-row :gutter="20" v-else>
          <el-col :span="6" v-for="product in products" :key="product.id">
            <el-card :body-style="{ padding: '0px' }" class="product-card" @click="goToDetail(product.id)">
              <img :src="product.imageUrl || 'https://via.placeholder.com/300x300'" class="product-image" />
              <div class="product-info">
                <h3>{{ product.name }}</h3>
                <p class="price">¥{{ product.price }}</p>
                <div class="product-meta">
                  <span class="sales">销量: {{ product.sales }}</span>
                  <span v-if="product.stock > 0" class="stock">库存: {{ product.stock }}</span>
                  <span v-else class="stock out-of-stock">缺货</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAllProducts } from '@/api/product'

const router = useRouter()
const loading = ref(true)
const products = ref([])

const fetchProducts = async () => {
  try {
    loading.value = true
    products.value = await getAllProducts()
  } catch (error) {
    console.error('获取商品列表失败:', error)
  } finally {
    loading.value = false
  }
}

const goToDetail = (id) => {
  router.push(`/product/${id}`)
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.products {
  max-width: 1200px;
  margin: 0 auto;
}

.product-list {
  min-height: 400px;
}

.product-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.3s;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.product-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.product-info {
  padding: 14px;
}

.product-info h3 {
  margin: 0 0 10px;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
  margin: 10px 0;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.sales {
  color: #909399;
}

.stock {
  color: #67c23a;
}

.stock.out-of-stock {
  color: #f56c6c;
}
</style>
