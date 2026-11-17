<template>
  <el-container class="main-layout">
    <!-- Header：仅保留 logo + 右侧导航，搜索栏集成在首页 -->
    <el-header class="header">
      <div class="header-content">
        <div class="logo">
          <router-link to="/">推荐商城</router-link>
        </div>

        <nav class="top-nav">
          <router-link to="/" class="nav-item">首页</router-link>
          <router-link to="/products" class="nav-item">商品列表</router-link>
          <router-link to="/chat" class="nav-item">AI导购</router-link>
          <router-link to="/recommend" class="nav-item">个性推荐</router-link>
          <router-link to="/orders" class="nav-item">我的订单</router-link>
          <router-link to="/cart" class="nav-item cart-link">
            <el-badge :value="cartCount" :hidden="cartCount === 0" class="cart-badge">
              <el-icon><ShoppingCart /></el-icon>
              购物车
            </el-badge>
          </router-link>
        </nav>

        <div class="user-info">
          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link">
              <el-avatar :size="28" class="user-avatar">
                {{ (userStore.userInfo?.nickname || userStore.userInfo?.username || '用')[0] }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view />
    </el-main>

    <el-footer class="footer">
      <div class="footer-inner">
        <div class="footer-links">
          <a href="#">关于我们</a>
          <a href="#">帮助中心</a>
          <a href="#">隐私政策</a>
          <a href="#">联系我们</a>
        </div>
        <p class="footer-copy">&copy; 2024 推荐商城 &nbsp;|&nbsp; 基于协同过滤算法的智能推荐系统</p>
      </div>
    </el-footer>
  </el-container>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCart } from '@/api/cart'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const cartCount = ref(0)

// 获取购物车数量
const fetchCartCount = async () => {
  try {
    const cartItems = await getCart()
    cartCount.value = cartItems.reduce((sum, item) => sum + item.quantity, 0)
  } catch (error) {
    console.error('获取购物车失败:', error)
  }
}

const handleCommand = async (command) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch {
      // 取消
    }
  }
}

// 监听路由变化，刷新购物车数量
watch(() => route.path, () => {
  if (userStore.token) fetchCartCount()
}, { immediate: true })
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* ===== Header ===== */
.header {
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 0;
  height: 54px !important;
  position: sticky;
  top: 0;
  z-index: 900;
}

.header-content {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  height: 100%;
  gap: 24px;
}

.logo a {
  text-decoration: none;
  font-size: 22px;
  font-weight: bold;
  color: #ff5000;
  letter-spacing: 1px;
  white-space: nowrap;
}

/* 导航 */
.top-nav {
  display: flex;
  align-items: center;
  gap: 0;
  flex: 1;
}

.nav-item {
  padding: 0 16px;
  height: 54px;
  line-height: 54px;
  font-size: 14px;
  color: #333;
  text-decoration: none;
  position: relative;
  transition: color 0.2s;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-item:hover {
  color: #ff5000;
}

.nav-item.router-link-active {
  color: #ff5000;
  font-weight: bold;
}

.nav-item.router-link-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 2px;
  background: #ff5000;
  border-radius: 1px;
}

.cart-link {
  display: flex;
  align-items: center;
}

.cart-link :deep(.el-badge) {
  display: flex;
  align-items: center;
}

.cart-link :deep(.el-badge__content) {
  transform: translateY(-50%) translateX(50%);
}

/* 用户信息 */
.user-info {
  flex-shrink: 0;
  margin-left: auto;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 6px;
  color: #333;
  font-size: 14px;
}

.el-dropdown-link:hover {
  color: #ff5000;
}

.user-avatar {
  background: #ff5000;
  color: #fff;
  font-size: 13px;
}

.user-name {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== Main Content ===== */
.main-content {
  padding: 0;
  background-color: #f5f5f5;
}

/* ===== Footer ===== */
.footer {
  background: #333;
  color: #aaa;
  padding: 24px 20px;
  height: auto !important;
}

.footer-inner {
  max-width: 1600px;
  margin: 0 auto;
  text-align: center;
}

.footer-links {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 12px;
}

.footer-links a {
  color: #aaa;
  text-decoration: none;
  font-size: 13px;
  transition: color 0.2s;
}

.footer-links a:hover {
  color: #fff;
}

.footer-copy {
  margin: 0;
  font-size: 13px;
}
</style>
