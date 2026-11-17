<template>
  <div class="session-sidebar">
    <div class="sidebar-header">
      <span>历史对话</span>
      <el-button size="small" type="primary" text @click="$emit('newChat')">
        <el-icon><Plus /></el-icon>
        新对话
      </el-button>
    </div>
    <div class="session-list">
      <div
        v-for="s in sessions"
        :key="s.id"
        class="session-item"
        :class="{ active: s.id === currentId }"
        @click="$emit('select', s.id)"
      >
        <div class="session-title">{{ s.title || '新的对话' }}</div>
        <div class="session-meta">
          <span class="session-time">{{ formatTime(s.updatedTime) }}</span>
          <el-button
            size="small"
            text
            type="danger"
            :icon="Delete"
            @click.stop="$emit('delete', s.id)"
          />
        </div>
      </div>
      <div v-if="sessions.length === 0" class="empty-hint">
        暂无历史对话
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, Delete } from '@element-plus/icons-vue'

defineProps({
  sessions: { type: Array, default: () => [] },
  currentId: { type: [Number, String], default: null }
})

defineEmits(['select', 'delete', 'newChat'])

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return d.toLocaleDateString()
}
</script>

<style scoped>
.session-sidebar {
  width: 260px;
  background: #fafafa;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  height: 100%;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #eee;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}
.session-item:hover { background: #fff0e8; }
.session-item.active { background: #ffe4d6; }

.session-title {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.session-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.session-time {
  font-size: 11px;
  color: #bbb;
}

.empty-hint {
  text-align: center;
  color: #ccc;
  font-size: 13px;
  padding: 40px 0;
}
</style>
