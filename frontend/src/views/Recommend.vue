<template>
  <div class="recommend">
    <el-card class="recommend-card">
      <template #header>
        <div class="card-header">
          <h2>
            <el-icon><Star /></el-icon>
            为您推荐
          </h2>
          <p class="subtitle">基于协同过滤算法的智能推荐</p>
        </div>
      </template>

      <div v-loading="loading" class="recommend-list">
        <el-empty v-if="!loading && recommendations.length === 0" description="暂无推荐数据" />

        <el-row :gutter="20" v-else>
          <el-col :span="6" v-for="(item, index) in recommendations" :key="item.productId">
            <el-card :body-style="{ padding: '0px' }" class="product-card" @click="goToDetail(item.productId)">
              <div class="rank-badge">{{ index + 1 }}</div>
              <img :src="item.productImage || 'https://via.placeholder.com/300x300'" class="product-image" />
              <div class="product-info">
                <h3>{{ item.productName }}</h3>
                <p class="price">¥{{ item.price }}</p>
                <div class="recommendation-info">
                  <el-tag type="success" size="small">推荐度: {{ item.score }}</el-tag>
                  <p class="reason">{{ item.reason }}</p>
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
import { ElMessage } from 'element-plus'
import { getRecommendations } from '@/api/recommendation'

const router = useRouter()
const loading = ref(true)
const recommendations = ref([])

const fetchRecommendations = async () => {
  try {
    loading.value = true
    recommendations.value = await getRecommendations(10)
    ElMessage.success('推荐已更新')
  } catch (error) {
    console.error('获取推荐失败:', error)
    ElMessage.error('获取推荐失败')
  } finally {
    loading.value = false
  }
}

const goToDetail = (id) => {
  router.push(`/product/${id}`)
}

onMounted(() => {
  fetchRecommendations()
})
</script>

<style scoped>
.recommend {
  max-width: 1200px;
  margin: 0 auto;
}

.recommend-card {
  margin: 20px 0;
}

.card-header {
  text-align: center;
}

.card-header h2 {
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #409eff;
}

.subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.recommend-list {
  min-height: 400px;
}

.product-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.3s;
  position: relative;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.rank-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  width: 30px;
  height: 30px;
  background: linear-gradient(135deg, #ff6b6b, #feca57);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  z-index: 1;
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

.recommendation-info {
  margin-top: 10px;
}

.reason {
  color: #909399;
  font-size: 12px;
  margin: 8px 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
