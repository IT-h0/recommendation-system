<template>
  <div class="chat-page">
    <!-- 侧栏 -->
    <ChatSessionList
      :sessions="chatStore.sessions"
      :current-id="chatStore.currentSessionId"
      @select="chatStore.selectSession($event)"
      @delete="chatStore.removeSession($event)"
      @new-chat="chatStore.newChat()"
    />

    <!-- 主对话区 -->
    <div class="chat-main">
      <div class="chat-header">
        <div class="chat-header-title">
          <el-icon><ChatDotRound /></el-icon>
          <span>{{ chatStore.currentSession?.title || 'AI 导购' }}</span>
        </div>
        <span class="chat-header-sub">说出你的需求，AI帮你挑选最合适的商品</span>
      </div>

      <div class="chat-messages" ref="messagesContainer">
        <!-- 欢迎语 -->
        <div v-if="chatStore.messages.length === 0" class="welcome-wrap">
          <div class="welcome-icon">
            <el-icon :size="48"><ChatDotRound /></el-icon>
          </div>
          <h2>你好，我是AI导购小荐</h2>
          <p>试试这样说:</p>
          <div class="example-chips">
            <span
              v-for="eg in examples"
              :key="eg"
              class="example-chip"
              @click="sendMessage(eg)"
            >{{ eg }}</span>
          </div>
        </div>

        <!-- 消息列表 -->
        <ChatMessageBubble
          v-for="(msg, i) in chatStore.messages"
          :key="i"
          :role="msg.role"
          :content="msg.content"
          :products="msg.products"
          :is-streaming="msg._streaming === true"
          @try-on="handleTryOn"
        />

        <!-- 流式指示器 -->
        <div v-if="chatStore.isStreaming && !chatStore.streamingContent" class="streaming-indicator">
          <span></span><span></span><span></span>
        </div>

        <div ref="scrollAnchor"></div>
      </div>

      <ChatInput
        ref="chatInputRef"
        :is-streaming="chatStore.isStreaming"
        @send="sendMessage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'
import { sendChatMessage } from '@/api/chat'
import ChatSessionList from '@/components/ChatSessionList.vue'
import ChatMessageBubble from '@/components/ChatMessageBubble.vue'
import ChatInput from '@/components/ChatInput.vue'

const chatStore = useChatStore()
const chatInputRef = ref(null)
const messagesContainer = ref(null)
const scrollAnchor = ref(null)
let abortController = null

const examples = [
  '我脸有点圆，想要一副复古的墨镜',
  '推荐几款性价比高的手机',
  '想买一件适合夏天穿的连衣裙',
  '送朋友什么礼物比较好，预算500以内'
]

function scrollToBottom() {
  nextTick(() => {
    scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

function sendMessage(text) {
  if (chatStore.isStreaming || !text.trim()) return

  if (abortController) {
    abortController.abort()
  }

  chatStore.addUserMessage(text)
  chatStore.startStreaming()
  scrollToBottom()

  abortController = sendChatMessage(
    text,
    chatStore.currentSessionId,
    {
      onToken(token) {
        chatStore.appendToken(token)
        scrollToBottom()
      },
      onProduct(product) {
        chatStore.addProduct(product)
        scrollToBottom()
      },
      onDone(sessionId) {
        chatStore.finishStreaming(sessionId)
        scrollToBottom()
      },
      onError(error) {
        chatStore.handleError(error)
        ElMessage.error(error)
      }
    }
  )
}

function handleTryOn(product) {
  ElMessage.info(`虚拟试穿功能即将上线: ${product.productName}`)
}

onMounted(async () => {
  await chatStore.loadSessions()
  chatStore.newChat()
  nextTick(() => chatInputRef.value?.focus())
})
</script>

<style scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 54px - 100px);
  max-width: 1400px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 14px 20px;
  border-bottom: 1px solid #eee;
  background: #fafafa;
  flex-shrink: 0;
}
.chat-header-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 700;
  color: #333;
}
.chat-header-sub {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
  display: block;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  scroll-behavior: smooth;
}

/* 欢迎区 */
.welcome-wrap {
  text-align: center;
  padding: 60px 20px;
}
.welcome-icon {
  color: #ff5000;
  margin-bottom: 12px;
}
.welcome-wrap h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #333;
}
.welcome-wrap p {
  color: #999;
  font-size: 14px;
  margin: 0 0 16px;
}
.example-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}
.example-chip {
  padding: 8px 16px;
  background: #fff5f0;
  border: 1px solid #ffe4d6;
  border-radius: 20px;
  font-size: 13px;
  color: #ff5000;
  cursor: pointer;
  transition: all 0.2s;
}
.example-chip:hover {
  background: #ff5000;
  color: #fff;
}

/* 流式动画 */
.streaming-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 14px;
}
.streaming-indicator span {
  width: 6px;
  height: 6px;
  background: #ff5000;
  border-radius: 50%;
  animation: bounce 1.2s infinite;
}
.streaming-indicator span:nth-child(2) { animation-delay: 0.2s; }
.streaming-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.3; }
  30% { transform: translateY(-6px); opacity: 1; }
}
</style>
