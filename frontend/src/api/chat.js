import request from './index'

const API_PREFIX = '/chat'

export const getSessions = () => request({ url: `${API_PREFIX}/sessions`, method: 'get' })

export const getMessages = (sessionId) =>
  request({ url: `${API_PREFIX}/sessions/${sessionId}/messages`, method: 'get' })

export const deleteSession = (sessionId) =>
  request({ url: `${API_PREFIX}/sessions/${sessionId}`, method: 'delete' })

/**
 * SSE 流式对话
 * @param {string} message 用户消息
 * @param {number|null} sessionId 会话ID
 * @param {object} callbacks { onToken, onProduct, onDone, onError }
 * @returns {AbortController} 用于取消请求
 */
export const sendChatMessage = (message, sessionId, callbacks) => {
  const token = localStorage.getItem('token')
  const controller = new AbortController()

  fetch('/api/chat/send', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ message, sessionId }),
    signal: controller.signal
  })
    .then(async (response) => {
      if (!response.ok) {
        const text = await response.text()
        callbacks.onError?.(`请求失败: ${response.status} ${text}`)
        return
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            try {
              const jsonStr = line.substring(5).trim()
              if (!jsonStr) continue
              const event = JSON.parse(jsonStr)

              switch (event.type) {
                case 'text':
                  callbacks.onToken?.(event.content)
                  break
                case 'product':
                  callbacks.onProduct?.({
                    productId: event.productId,
                    productName: event.productName,
                    productImage: event.productImage,
                    price: event.price,
                    score: event.score,
                    reason: event.reason,
                    tryOnSupported: event.tryOnSupported
                  })
                  break
                case 'done':
                  callbacks.onDone?.(event.sessionId)
                  break
                case 'error':
                  callbacks.onError?.(event.content)
                  break
              }
            } catch (e) {
              // Skip unparseable lines
            }
          }
        }
      }
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        callbacks.onError?.(err.message || '网络错误')
      }
    })

  return controller
}
