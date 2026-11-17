<template>
  <div class="chat-input-area">
    <textarea
      ref="textareaRef"
      v-model="inputText"
      class="chat-textarea"
      :placeholder="isStreaming ? 'AI正在回复中...' : '输入你的需求,例如: 我脸有点圆,想要一副复古的墨镜'"
      :disabled="isStreaming"
      rows="1"
      @keydown.enter.exact.prevent="send"
      @input="autoResize"
    ></textarea>
    <button
      class="send-btn"
      :class="{ disabled: !canSend }"
      :disabled="!canSend"
      @click="send"
    >
      <el-icon><Promotion /></el-icon>
    </button>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

const props = defineProps({
  isStreaming: { type: Boolean, default: false }
})

const emit = defineEmits(['send'])

const inputText = ref('')
const textareaRef = ref(null)

const canSend = computed(() => inputText.value.trim() && !props.isStreaming)

function send() {
  const text = inputText.value.trim()
  if (!text || props.isStreaming) return
  emit('send', text)
  inputText.value = ''
  nextTick(() => autoResize())
}

function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

defineExpose({ focus: () => textareaRef.value?.focus() })
</script>

<style scoped>
.chat-input-area {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 14px 18px;
  background: #fff;
  border-top: 1px solid #eee;
}

.chat-textarea {
  flex: 1;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
  max-height: 120px;
}
.chat-textarea:focus {
  border-color: #ff5000;
}
.chat-textarea:disabled {
  background: #f9f9f9;
  cursor: not-allowed;
}

.send-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #ff5000;
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
  font-size: 18px;
}
.send-btn:hover {
  background: #ff3300;
  transform: scale(1.05);
}
.send-btn.disabled {
  background: #ddd;
  cursor: not-allowed;
  transform: none;
}
</style>
