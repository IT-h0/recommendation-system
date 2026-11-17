<template>
  <div class="message-row" :class="roleClass">
    <el-avatar v-if="role === 'assistant'" :size="32" class="msg-avatar bot-avatar">AI</el-avatar>
    <div class="message-bubble" :class="bubbleClass">
      <!-- 产品卡片（文字上方） -->
      <div v-if="products && products.length > 0" class="product-list">
        <ChatProductCard
          v-for="(p, i) in products"
          :key="i"
          :product="p"
          @tryOn="(p) => $emit('tryOn', p)"
        />
      </div>
      <!-- 文本内容 -->
      <div class="message-text">{{ content }}</div>
      <!-- 流式输出时闪烁光标 -->
      <span v-if="isStreaming" class="typing-cursor">|</span>
    </div>
    <el-avatar v-if="role === 'user'" :size="32" class="msg-avatar user-avatar">
      {{ userInitial }}
    </el-avatar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import ChatProductCard from '@/components/ChatProductCard.vue'

const props = defineProps({
  role: { type: String, required: true },
  content: { type: String, default: '' },
  isStreaming: { type: Boolean, default: false },
  products: { type: Array, default: () => [] }
})

defineEmits(['tryOn'])

const userStore = useUserStore()

const roleClass = computed(() => (props.role === 'user' ? 'user-row' : 'bot-row'))
const bubbleClass = computed(() => (props.role === 'user' ? 'user-bubble' : 'bot-bubble'))
const userInitial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || '用'
  return name[0]
})
</script>

<style scoped>
.message-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: flex-start;
}
.user-row { flex-direction: row-reverse; }
.bot-row { flex-direction: row; }

.msg-avatar {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
}
.bot-avatar {
  background: linear-gradient(135deg, #ff5000, #ff8c00);
  color: #fff;
}
.user-avatar {
  background: #409eff;
  color: #fff;
}

.message-bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 12px;
  position: relative;
}
.user-bubble {
  background: #ff5000;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.bot-bubble {
  background: #f5f5f5;
  color: #333;
  border-bottom-left-radius: 4px;
  border: 1px solid #eee;
}

.message-text {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.user-bubble .message-text {
  color: #fff;
}

.product-list {
  margin-bottom: 8px;
}

.typing-cursor {
  display: inline-block;
  animation: blink 1s step-end infinite;
  color: #ff5000;
  font-weight: bold;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
