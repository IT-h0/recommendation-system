import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getSessions, getMessages, deleteSession as apiDeleteSession } from '@/api/chat'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref([])
  const isStreaming = ref(false)
  const streamingContent = ref('')
  const streamingProducts = ref([])

  const currentSession = computed(() =>
    sessions.value.find((s) => s.id === currentSessionId.value)
  )

  async function loadSessions() {
    try {
      const res = await getSessions()
      sessions.value = res || []
    } catch (e) {
      console.error('加载会话列表失败', e)
    }
  }

  async function loadMessages(sessionId) {
    try {
      const res = await getMessages(sessionId)
      messages.value = (res || []).map((m) => ({
        ...m,
        products: parseProducts(m.metadata)
      }))
    } catch (e) {
      console.error('加载消息失败', e)
    }
  }

  function parseProducts(metadata) {
    if (!metadata) return []
    try {
      const parsed = typeof metadata === 'string' ? JSON.parse(metadata) : metadata
      return parsed.products || []
    } catch {
      return []
    }
  }

  function selectSession(sessionId) {
    currentSessionId.value = sessionId
    messages.value = []
    loadMessages(sessionId)
  }

  function newChat() {
    currentSessionId.value = null
    messages.value = []
    streamingContent.value = ''
    streamingProducts.value = []
  }

  function addUserMessage(text) {
    messages.value.push({ role: 'user', content: text, products: [] })
  }

  function startStreaming() {
    isStreaming.value = true
    streamingContent.value = ''
    streamingProducts.value = []
    messages.value.push({
      role: 'assistant',
      content: '',
      products: [],
      _streaming: true
    })
  }

  function appendToken(token) {
    streamingContent.value += token
    const last = messages.value[messages.value.length - 1]
    if (last && last._streaming) {
      last.content = streamingContent.value
    }
  }

  function addProduct(product) {
    streamingProducts.value.push(product)
    const last = messages.value[messages.value.length - 1]
    if (last && last._streaming) {
      last.products = [...streamingProducts.value]
    }
  }

  function finishStreaming(sessionId) {
    const last = messages.value[messages.value.length - 1]
    if (last && last._streaming) {
      last._streaming = false
    }
    isStreaming.value = false
    streamingContent.value = ''
    streamingProducts.value = []

    if (sessionId && !currentSessionId.value) {
      currentSessionId.value = sessionId
      loadSessions()
    }
  }

  function handleError(error) {
    isStreaming.value = false
    const last = messages.value[messages.value.length - 1]
    if (last && last._streaming) {
      last.content = `[错误] ${error}`
      last._streaming = false
    }
  }

  async function removeSession(sessionId) {
    try {
      await apiDeleteSession(sessionId)
      if (currentSessionId.value === sessionId) {
        newChat()
      }
      await loadSessions()
    } catch (e) {
      console.error('删除会话失败', e)
    }
  }

  return {
    sessions,
    currentSessionId,
    messages,
    isStreaming,
    streamingContent,
    currentSession,
    loadSessions,
    loadMessages,
    selectSession,
    newChat,
    addUserMessage,
    startStreaming,
    appendToken,
    addProduct,
    finishStreaming,
    handleError,
    removeSession
  }
})
