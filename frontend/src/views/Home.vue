<template>
  <!-- 点击页面其他区域关闭搜索下拉 -->
  <div class="home" @click.self="showSearchDropdown = false">

    <!-- ===== 顶部搜索栏 ===== -->
    <div class="search-bar" :class="{ 'search-bar-fixed': isScrolled }">
      <div class="search-bar-inner">
        <div class="search-box">
          <div class="search-input-wrap">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品..."
              @input="onSearchInput"
              @focus="onSearchFocus"
              @blur="onSearchBlur"
              @keyup.enter="doSearch"
              clearable
              size="large"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <!-- 搜索建议下拉 -->
            <div v-if="showSearchDropdown && searchSuggestions.length > 0" class="search-dropdown">
              <div
                v-for="item in searchSuggestions"
                :key="item.id"
                class="suggestion-item"
                @mousedown.prevent="goToDetail(item.id)"
              >
                <el-icon class="suggestion-icon"><Search /></el-icon>
                <span>{{ item.name }}</span>
              </div>
            </div>
          </div>
          <button class="ai-guide-btn" @click="goToChat">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI导购</span>
          </button>
        </div>

        <!-- 快捷入口 -->
        <div class="quick-links">
          <router-link to="/recommend" class="quick-link">个性推荐</router-link>
          <router-link to="/cart" class="quick-link">购物车</router-link>
        </div>
      </div>
    </div>

    <!-- 搜索栏固定时的占位元素 -->
    <div v-if="isScrolled" class="search-bar-placeholder"></div>

    <!-- ===== 横向分类标签栏 ===== -->
    <div class="category-tabs">
      <div class="category-tabs-inner">
        <span
          class="tab-item"
          :class="{ active: selectedCategoryId === null }"
          @click="selectCategory(null, '全部商品')"
        >全部</span>
        <span
          v-for="parent in categoryTree"
          :key="parent.id"
          class="tab-item"
          :class="{ active: selectedParentId === parent.id }"
          @click="selectCategory(parent.id, parent.name, true)"
        >{{ parent.name }}</span>
      </div>
    </div>

    <!-- ===== 主内容区 ===== -->
    <div class="main-area">

      <!-- 左侧分类导航 -->
      <div
        class="left-sidebar"
        @mouseenter="isSidebarOpen = true"
        @mouseleave="isSidebarOpen = false"
      >
        <div class="sidebar-header">
          <el-icon><Menu /></el-icon>
          <span>全部分类</span>
        </div>

        <div class="sidebar-list">
          <div
            v-for="parent in categoryTree"
            :key="parent.id"
            class="sidebar-item"
            :class="{ 'sidebar-item-active': hoveredParentId === parent.id }"
            @mouseenter="hoveredParentId = parent.id"
            @mouseleave="hoveredParentId = null"
            @click="selectCategory(parent.id, parent.name, true)"
          >
            <span class="sidebar-item-name">{{ parent.name }}</span>
            <el-icon v-if="parent.children && parent.children.length"><ArrowRight /></el-icon>

            <!-- 二级分类飞出面板 -->
            <transition name="fly-out">
              <div
                v-if="hoveredParentId === parent.id && parent.children && parent.children.length"
                class="sub-panel"
              >
                <div class="sub-panel-title">{{ parent.name }}</div>
                <div class="sub-panel-grid">
                  <span
                    v-for="child in parent.children"
                    :key="child.id"
                    class="sub-item"
                    @click.stop="selectCategory(child.id, child.name)"
                  >{{ child.name }}</span>
                </div>
              </div>
            </transition>
          </div>
        </div>
      </div>

      <!-- 右侧内容：轮播 + 商品 -->
      <div class="right-area">

        <!-- 轮播图 -->
        <el-carousel
          :interval="4500"
          height="360px"
          indicator-position="outside"
          class="banner"
        >
          <el-carousel-item v-for="(b, i) in banners" :key="i">
            <div class="banner-slide" :style="{ background: b.bg }">
              <div class="banner-text">
                <h2>{{ b.title }}</h2>
                <p>{{ b.sub }}</p>
              </div>
              <img :src="b.img" class="banner-img" v-if="b.img" />
            </div>
          </el-carousel-item>
        </el-carousel>

        <!-- 分类二级导航（选中一级分类后显示） -->
        <div v-if="subCategories.length > 0" class="sub-category-bar">
          <span class="sub-bar-label">{{ currentParentName }}：</span>
          <span
            v-for="sub in subCategories"
            :key="sub.id"
            class="sub-bar-item"
            :class="{ active: selectedCategoryId === sub.id }"
            @click="selectCategory(sub.id, sub.name)"
          >{{ sub.name }}</span>
        </div>

        <!-- 商品列表区 -->
        <div class="product-section">
          <div class="product-header">
            <h3 class="section-title">
              <span class="title-bar"></span>
              {{ currentCategoryName }}
              <span class="product-count">（{{ filteredProducts.length }} 件）</span>
            </h3>
            <div class="sort-bar">
              <span class="sort-label">排序</span>
              <span
                v-for="s in sortOptions"
                :key="s.value"
                class="sort-item"
                :class="{ active: sortType === s.value }"
                @click="sortType = s.value"
              >{{ s.label }}</span>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="loading" class="loading-wrap">
            <el-skeleton :rows="3" animated />
          </div>

          <!-- 商品网格 -->
          <div v-else-if="filteredProducts.length > 0" class="product-grid">
            <div
              v-for="product in filteredProducts"
              :key="product.id"
              class="product-card"
              @click="goToDetail(product.id)"
            >
              <div class="card-img-wrap">
                <img
                  :src="product.imageUrl || defaultImg"
                  class="card-img"
                  @error="(e) => (e.target.src = defaultImg)"
                  loading="lazy"
                />
                <span v-if="product.categoryName" class="card-badge">{{ product.categoryName }}</span>
              </div>
              <div class="card-body">
                <div class="card-name">{{ product.name }}</div>
                <div class="card-footer">
                  <div class="card-price">
                    <span class="price-sym">¥</span>
                    <span class="price-num">{{ product.price }}</span>
                  </div>
                  <div class="card-sales">已售 {{ product.sales || 0 }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="empty-wrap">
            <el-empty :description="emptyText" />
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 回到顶部按钮 ===== -->
    <transition name="fade">
      <div
        v-if="isScrolled"
        class="back-to-top"
        @click="scrollToTop"
        title="回到顶部"
      >
        <el-icon><ArrowUp /></el-icon>
        <span>顶部</span>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Search } from '@element-plus/icons-vue'
import { getAllProducts, getAllCategories, getCategoryTree, searchProducts } from '@/api/product'

const router = useRouter()

/* ---- 常量 ---- */
const defaultImg = 'https://via.placeholder.com/200x200?text=No+Image'

const banners = [
  { title: '智能推荐好物', sub: '协同过滤算法为你专属筛选', bg: 'linear-gradient(135deg,#ff6b35 0%,#f7c59f 100%)', img: '' },
  { title: '数码科技潮品', sub: '最新智能设备一站购齐', bg: 'linear-gradient(135deg,#667eea 0%,#764ba2 100%)', img: '' },
  { title: '时尚穿搭新款', sub: '潮流单品限时特惠', bg: 'linear-gradient(135deg,#f093fb 0%,#f5576c 100%)', img: '' },
  { title: '品质家居生活', sub: '打造温馨舒适的家', bg: 'linear-gradient(135deg,#4facfe 0%,#00f2fe 100%)', img: '' },
]

const sortOptions = [
  { label: '综合', value: 'default' },
  { label: '销量', value: 'sales' },
  { label: '价格↑', value: 'price_asc' },
  { label: '价格↓', value: 'price_desc' },
]

/* ---- 响应式数据 ---- */
const allProducts = ref([])
const categoryTree = ref([])   // [{id, name, children:[...]}]
const loading = ref(false)
const searchKeyword = ref('')
const searchSuggestions = ref([])
const showSearchDropdown = ref(false)
const isScrolled = ref(false)
const isSidebarOpen = ref(false)
const hoveredParentId = ref(null)
const selectedCategoryId = ref(null)   // 当前选中的分类 id（二级）
const selectedParentId = ref(null)     // 当前选中的一级 id
const currentCategoryName = ref('全部商品')
const currentParentName = ref('')
const sortType = ref('default')
const isSearchMode = ref(false)  // 是否处于关键字搜索结果模式
const searchResults = ref([])
const emptyText = ref('该分类下暂无商品')
let searchTimer = null

/* ---- 计算属性 ---- */
// 当前一级分类的子分类列表
const subCategories = computed(() => {
  if (!selectedParentId.value) return []
  const parent = categoryTree.value.find(p => p.id === selectedParentId.value)
  return parent?.children || []
})

// 过滤 + 排序商品
const filteredProducts = computed(() => {
  let list = isSearchMode.value ? [...searchResults.value] : [...allProducts.value]

  // 按分类过滤（仅在非搜索模式下）
  if (!isSearchMode.value) {
    if (selectedCategoryId.value) {
      // 选了二级分类
      list = list.filter(p => p.categoryId === selectedCategoryId.value)
    } else if (selectedParentId.value) {
      // 只选了一级分类 → 展示该一级下所有二级分类的商品
      const parent = categoryTree.value.find(p => p.id === selectedParentId.value)
      const childIds = (parent?.children || []).map(c => c.id)
      if (childIds.length) {
        list = list.filter(p => childIds.includes(p.categoryId))
      }
    }
  }

  // 排序
  switch (sortType.value) {
    case 'sales':
      list.sort((a, b) => (b.sales || 0) - (a.sales || 0)); break
    case 'price_asc':
      list.sort((a, b) => Number(a.price) - Number(b.price)); break
    case 'price_desc':
      list.sort((a, b) => Number(b.price) - Number(a.price)); break
  }
  return list
})

/* ---- 方法 ---- */
const fetchAll = async () => {
  loading.value = true
  try {
    const [products, tree] = await Promise.all([getAllProducts(), getCategoryTree()])
    allProducts.value = products || []
    categoryTree.value = tree || []
  } catch (e) {
    console.error('初始化数据失败', e)
  } finally {
    loading.value = false
  }
}

const selectCategory = (id, name, isParent = false) => {
  isSearchMode.value = false
  searchKeyword.value = ''
  searchSuggestions.value = []
  sortType.value = 'default'
  isSidebarOpen.value = false
  hoveredParentId.value = null

  if (id === null) {
    selectedCategoryId.value = null
    selectedParentId.value = null
    currentCategoryName.value = '全部商品'
    currentParentName.value = ''
    return
  }

  if (isParent) {
    selectedParentId.value = id
    selectedCategoryId.value = null  // 先不选二级
    currentParentName.value = name
    currentCategoryName.value = name
  } else {
    selectedCategoryId.value = id
    currentCategoryName.value = name
  }
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 搜索输入防抖
const onSearchInput = () => {
  clearTimeout(searchTimer)
  const kw = searchKeyword.value.trim()
  if (!kw) { searchSuggestions.value = []; return }
  searchTimer = setTimeout(async () => {
    try {
      const res = await searchProducts(kw)
      searchSuggestions.value = (res || []).slice(0, 8)
    } catch { searchSuggestions.value = [] }
  }, 300)
}

const onSearchFocus = () => {
  if (searchKeyword.value.trim()) showSearchDropdown.value = true
}

const onSearchBlur = () => {
  setTimeout(() => { showSearchDropdown.value = false }, 200)
}

const doSearch = async () => {
  const kw = searchKeyword.value.trim()
  if (!kw) { ElMessage.warning('请输入搜索关键词'); return }
  showSearchDropdown.value = false
  loading.value = true
  try {
    const res = await searchProducts(kw)
    searchResults.value = res || []
    isSearchMode.value = true
    selectedCategoryId.value = null
    selectedParentId.value = null
    currentCategoryName.value = `"${kw}" 的搜索结果`
    emptyText.value = `未找到与 "${kw}" 相关的商品`
    sortType.value = 'default'
  } catch (e) {
    console.error('搜索失败', e)
  } finally {
    loading.value = false
  }
}

const goToDetail = (id) => {
  showSearchDropdown.value = false
  router.push(`/product/${id}`)
}

const goToChat = () => {
  router.push('/chat')
}

// 滚动监听 → 搜索栏悬浮 + 回到顶部按钮显示
const onScroll = () => { isScrolled.value = window.scrollY > 60 }

// 回到顶部
const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 自定义指令：点击外部关闭
const vClickOutside = {
  mounted(el, binding) {
    el._clickOutside = (e) => { if (!el.contains(e.target)) binding.value(e) }
    document.addEventListener('click', el._clickOutside)
  },
  unmounted(el) {
    document.removeEventListener('click', el._clickOutside)
  }
}

// 搜索关键词变化时同步下拉
watch(searchKeyword, (val) => {
  if (!val.trim()) { showSearchDropdown.value = false; searchSuggestions.value = [] }
  else showSearchDropdown.value = true
})

onMounted(() => {
  fetchAll()
  window.addEventListener('scroll', onScroll)
})
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  clearTimeout(searchTimer)
})
</script>

<style scoped>
/* ============================================================
   页面容器
============================================================ */
.home {
  min-height: 100vh;
  background: #f5f5f5;
}

/* ============================================================
   搜索栏
============================================================ */
.search-bar {
  background: linear-gradient(90deg, #ff5000 0%, #ff7d00 100%);
  padding: 12px 0;
  transition: all 0.3s;
  position: relative;
  z-index: 500;
}
.search-bar-fixed {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  box-shadow: 0 3px 14px rgba(255, 80, 0, 0.35);
  animation: slideDown 0.3s ease;
}
@keyframes slideDown {
  from { transform: translateY(-100%); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
.search-bar-placeholder {
  height: 60px;
}
.search-bar-inner {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
}
.logo-text {
  font-size: 26px;
  font-weight: 900;
  color: #fff;
  text-decoration: none;
  letter-spacing: 2px;
  white-space: nowrap;
  flex-shrink: 0;
  display: flex;
  align-items: baseline;
}
.logo-main { color: #fff; }
.logo-sub { color: #ffe4d6; }

/* 搜索框 */
.search-box {
  flex: 0 1 760px;
  max-width: 760px;
  width: 100%;
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.search-input-wrap {
  flex: 1;
  position: relative;
}
.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,.12);
  z-index: 200;
  margin-top: 2px;
  overflow: hidden;
}
.suggestion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s;
}
.suggestion-item:hover {
  background: #fff5f0;
  color: #ff5000;
}
.suggestion-icon {
  font-size: 14px;
  color: #bbb;
  flex-shrink: 0;
}
/* AI导购按钮 */
.ai-guide-btn {
  background: linear-gradient(135deg, #ff5000, #ff7a00);
  border: none;
  color: #fff;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 24px;
  transition: opacity 0.25s, transform 0.2s;
  white-space: nowrap;
  flex-shrink: 0;
  height: 40px;
}
.ai-guide-btn:hover {
  opacity: 0.9;
  transform: scale(1.03);
}

/* 快捷入口 */
.quick-links {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
  position: absolute;
  right: 24px;
}
.quick-link {
  color: rgba(255,255,255,0.9);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;
}
.quick-link:hover { color: #fff; text-decoration: underline; }

/* 动画 */
.slide-fade-enter-active, .slide-fade-leave-active { transition: all .2s ease; }
.slide-fade-enter-from, .slide-fade-leave-to { opacity: 0; transform: translateY(-6px); }

/* ============================================================
   横向分类标签栏
============================================================ */
.category-tabs {
  background: #fff;
  border-bottom: 2px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0,0,0,.05);
}
.category-tabs-inner {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  overflow-x: auto;
  scrollbar-width: none;
}
.category-tabs-inner::-webkit-scrollbar { display: none; }
.tab-item {
  padding: 12px 20px;
  font-size: 14px;
  color: #555;
  cursor: pointer;
  white-space: nowrap;
  position: relative;
  transition: color 0.25s;
  user-select: none;
}
.tab-item:hover { color: #ff5000; }
.tab-item.active {
  color: #ff5000;
  font-weight: 700;
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 20%;
  right: 20%;
  height: 2px;
  background: #ff5000;
  border-radius: 1px;
}

/* ============================================================
   主内容区
============================================================ */
.main-area {
  width: 100%;
  max-width: 1600px;
  margin: 16px auto;
  padding: 0 24px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
  box-sizing: border-box;
}

/* ============================================================
   左侧分类导航
============================================================ */
.left-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  overflow: visible;
  box-shadow: 0 2px 8px rgba(0,0,0,.06);
  position: relative;
  /* 与轮播图等高：轮播 360px + 底部 indicator 36px */
  height: 396px;
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  background: linear-gradient(135deg, #ff5000, #ff8c00);
  color: #fff;
  padding: 13px 16px;
  font-size: 15px;
  font-weight: 700;
  border-radius: 8px 8px 0 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.sidebar-list {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #ffd0b8 transparent;
}
.sidebar-list::-webkit-scrollbar { width: 4px; }
.sidebar-list::-webkit-scrollbar-thumb { background: #ffd0b8; border-radius: 2px; }
.sidebar-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  position: relative;
  transition: background 0.2s, color 0.2s;
}
.sidebar-item:last-child { border-bottom: none; border-radius: 0 0 8px 8px; }
.sidebar-item:hover,
.sidebar-item-active { background: #fff5f0; color: #ff5000; }
.sidebar-item-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 二级飞出面板 */
.sub-panel {
  position: absolute;
  left: 100%;
  top: -1px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  box-shadow: 4px 4px 20px rgba(0,0,0,.12);
  padding: 16px;
  min-width: 320px;
  z-index: 400;
  margin-left: 4px;
}
.sub-panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #ff5000;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #ffd0b8;
}
.sub-panel-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.sub-item {
  padding: 6px 14px;
  background: #f7f7f7;
  border-radius: 4px;
  font-size: 13px;
  color: #444;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.sub-item:hover { background: #fff0e8; color: #ff5000; }

.fly-out-enter-active, .fly-out-leave-active { transition: opacity .18s, transform .18s; }
.fly-out-enter-from, .fly-out-leave-to { opacity: 0; transform: translateX(-8px); }

/* ============================================================
   右侧内容区
============================================================ */
.right-area { flex: 1; min-width: 0; }

/* 轮播 */
.banner { border-radius: 10px; overflow: hidden; margin-bottom: 16px; }
.banner-slide {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.banner-text { text-align: center; color: #fff; z-index: 1; }
.banner-text h2 { font-size: 38px; margin: 0 0 12px; text-shadow: 0 2px 8px rgba(0,0,0,.2); }
.banner-text p { font-size: 18px; opacity: .9; margin: 0; }
.banner-img { position: absolute; right: 60px; height: 80%; object-fit: contain; opacity: .6; }

/* 二级分类条 */
.sub-category-bar {
  background: #fff;
  border-radius: 8px;
  padding: 10px 16px;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  box-shadow: 0 1px 6px rgba(0,0,0,.05);
}
.sub-bar-label { font-size: 13px; color: #999; flex-shrink: 0; }
.sub-bar-item {
  padding: 5px 14px;
  border-radius: 14px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  border: 1px solid #eee;
  transition: all 0.2s;
}
.sub-bar-item:hover { border-color: #ff5000; color: #ff5000; }
.sub-bar-item.active { background: #ff5000; color: #fff; border-color: #ff5000; }

/* ============================================================
   商品区
============================================================ */
.product-section {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  min-height: 420px;
}
.product-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}
.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #222;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-bar {
  display: inline-block;
  width: 4px;
  height: 18px;
  background: #ff5000;
  border-radius: 2px;
  flex-shrink: 0;
}
.product-count { font-size: 13px; color: #aaa; font-weight: 400; }

.sort-bar { display: flex; align-items: center; gap: 4px; }
.sort-label { font-size: 13px; color: #999; margin-right: 4px; }
.sort-item {
  padding: 5px 12px;
  font-size: 13px;
  color: #555;
  border: 1px solid #eee;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}
.sort-item:hover { color: #ff5000; border-color: #ff5000; }
.sort-item.active { background: #ff5000; color: #fff; border-color: #ff5000; }

/* 商品网格 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.product-card {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s, border-color 0.25s;
  background: #fff;
}
.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(255, 80, 0, 0.12);
  border-color: #ff5000;
}
.card-img-wrap {
  position: relative;
  width: 100%;
  padding-top: 100%;
  background: #fafafa;
  overflow: hidden;
}
.card-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.product-card:hover .card-img { transform: scale(1.06); }
.card-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(255,80,0,.82);
  color: #fff;
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 3px;
}
.card-body { padding: 12px; }
.card-name {
  font-size: 13px;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 38px;
  line-height: 19px;
  margin-bottom: 8px;
}
.card-footer { display: flex; justify-content: space-between; align-items: flex-end; }
.card-price { color: #ff5000; line-height: 1; }
.price-sym { font-size: 12px; }
.price-num { font-size: 20px; font-weight: 700; }
.card-sales { font-size: 11px; color: #bbb; }

.loading-wrap { padding: 40px 0; }
.empty-wrap { padding: 60px 0; }

/* ============================================================
   回到顶部按钮
============================================================ */
.back-to-top {
  position: fixed;
  right: 24px;
  bottom: 40px;
  width: 48px;
  height: 48px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 4px 16px rgba(0,0,0,.15);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 999;
  transition: all 0.3s ease;
  color: #666;
  font-size: 12px;
  gap: 2px;
}
.back-to-top:hover {
  background: #ff5000;
  color: #fff;
  transform: translateY(-4px);
  box-shadow: 0 6px 20px rgba(255,80,0,.35);
}
.back-to-top .el-icon {
  font-size: 18px;
}

/* 回到顶部动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(10px); }
</style>
