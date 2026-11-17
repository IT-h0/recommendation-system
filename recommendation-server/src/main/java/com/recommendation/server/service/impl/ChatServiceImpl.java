package com.recommendation.server.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.recommendation.common.exception.BusinessException;
import com.recommendation.pojo.dto.ChatRequestDTO;
import com.recommendation.pojo.entity.ChatSession;
import com.recommendation.pojo.vo.ChatMessageVO;
import com.recommendation.pojo.vo.ChatSessionVO;
import com.recommendation.server.ai.DeepSeekClient;
import com.recommendation.server.ai.ProductContextBuilder;
import com.recommendation.server.ai.tool.ShoppingAssistantTools;
import com.recommendation.server.ai.tool.UserContextHolder;
import com.recommendation.server.mapper.ChatMessageMapper;
import com.recommendation.server.mapper.ChatSessionMapper;
import com.recommendation.server.service.ChatService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

// ChatMessage 导入的是 LangChain4j 接口（消息上下文），DB 实体
// com.recommendation.pojo.entity.ChatMessage 使用全限定名，避免同名冲突
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    // 同步模型用于工具调用循环，流式模型用于最终文本 SSE 输出
    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingModel;
    private final ShoppingAssistantTools tools;
    private final UserContextHolder userContext;
    private final ProductContextBuilder productContextBuilder;
    private final DeepSeekClient deepSeekClient;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    // 限制工具调用轮次，防止 LLM 陷入无限 tool-call 循环或上下文过长
    private static final int MAX_FUNCTION_LOOPS = 3;
    // 一次对话最多展示的商品卡片数（多次工具调用结果去重后封顶）
    private static final int MAX_PRODUCT_CARDS = 3;

    @Override
    public SseEmitter chat(ChatRequestDTO request, Long userId) {
        // SSE 超时设为 120s：覆盖 LLM 最长响应时间（60s timeout + 工具调用开销）
        SseEmitter emitter = new SseEmitter(120_000L);

        // 核心逻辑跑在异步线程中，chat() 方法立即返回 SseEmitter，不阻塞 Tomcat 线程
        CompletableFuture.runAsync(() -> {
            try {
                // userContext 通过 ThreadLocal 传递 userId，使 @Tool 方法无需参数即可获取当前用户
                userContext.set(userId);

                ChatSession session = findOrCreateSession(request.getSessionId(), userId);
                boolean isNewSession = request.getSessionId() == null;
                // 先持久化用户消息再调用 LLM，避免 LLM 异常导致消息丢失
                saveMessage(session.getId(), "user", request.getMessage(), null);

                if (isNewSession) {
                    generateTitleFromMessage(session.getId(), request.getMessage());
                }

                // 构建消息列表：system prompt（含商品库上下文） + 最近 20 条历史
                List<ChatMessage> messages = buildLlmMessages(session.getId());
                // 工具调用过程中收集的商品数据，用于后续 SSE product 事件
                List<JSONObject> retrievedProducts = new ArrayList<>();
                List<ToolSpecification> toolSpecs = tools.getSpecifications();

                // 工具调用循环：不用 AiServices（它隐藏了中间工具结果），
                // 而是手动轮询 sync 模型，这样才能在工具返回的商品中提取 product 数据
                boolean hasToolCall = true;
                int loopCount = 0;
                while (hasToolCall && loopCount < MAX_FUNCTION_LOOPS) {
                    loopCount++;
                    // 用原生 HTTP 客户端直连 DeepSeek（标准 tools 参数），根除 DSML 问题
                    AiMessage aiMessage = deepSeekClient.chat(messages, toolSpecs);

                    if (aiMessage.hasToolExecutionRequests()) {
                        // 将 LLM 返回的 tool-call 消息加入上下文，LangChain4j 的 AiMessage
                        // 已包含完整的 ToolExecutionRequest 列表
                        messages.add(aiMessage);

                        for (ToolExecutionRequest toolReq : aiMessage.toolExecutionRequests()) {
                            String result = tools.execute(toolReq.name(), toolReq.arguments());
                            // 从工具结果中收集商品数据，用于后续 SSE product 事件
                            collectProducts(result, retrievedProducts);
                            // ToolExecutionResultMessage 通过 toolCallId 与上文的
                            // assistant tool-call 消息关联，LLM 据此匹配请求与结果
                            messages.add(ToolExecutionResultMessage.from(
                                    toolReq.id(), toolReq.name(), result));
                        }
                    } else {
                        hasToolCall = false;
                    }
                }

                streamFinalResponse(messages, emitter, retrievedProducts, session.getId());

            } catch (Exception e) {
                log.error("Chat error for user {}", userId, e);
                sendErrorEvent(emitter, e.getMessage());
                // 只用 complete() 不用 completeWithError()：后者会触发 Spring MVC
                // 异常链，尝试将 Result 序列化为 text/event-stream 时失败
                emitter.complete();
            } finally {
                // 清理 ThreadLocal，防止线程池复用时的数据串扰
                userContext.clear();
            }
        });

        return emitter;
    }

    // 先发送产品卡片事件，再启动流式文本输出，保证前端先渲染商品卡片后滚动对话
    private void streamFinalResponse(List<ChatMessage> messages, SseEmitter emitter,
                                     List<JSONObject> products, Long sessionId) {
        // 去重 + 封顶，避免多次工具调用累计出过多商品卡片
        final List<JSONObject> finalProducts = dedupeAndLimitProducts(products);
        try {
            for (JSONObject product : finalProducts) {
                sendProductEvent(emitter, product);
            }

            // 用 StringBuilder 收集完整响应文本，在 onCompleteResponse 中入库
            StringBuilder fullContent = new StringBuilder();

            // streamingModel.chat() 异步执行，回调在 Netty/OkHttp IO 线程中触发
            streamingModel.chat(messages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String token) {
                    if (token != null && !token.isEmpty()) {
                        fullContent.append(token);
                        sendTextEvent(emitter, token);
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    String metadata = finalProducts.isEmpty() ? null
                            : JSON.toJSONString(Map.of("products", finalProducts));
                    saveMessage(sessionId, "assistant", fullContent.toString(), metadata);
                    sendDoneEvent(emitter, sessionId);
                    emitter.complete();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Stream error", error);
                    sendErrorEvent(emitter, "流式响应中断: " + error.getMessage());
                    sendDoneEvent(emitter, sessionId);
                    emitter.complete();
                }
            });

        } catch (Exception e) {
            log.error("Stream setup error", e);
            sendErrorEvent(emitter, "流式响应启动失败: " + e.getMessage());
            sendDoneEvent(emitter, sessionId);
            emitter.complete();
        }
    }

    private void collectProducts(String functionResult, List<JSONObject> collected) {
        try {
            Object parsed = JSON.parse(functionResult);
            if (parsed instanceof JSONArray arr) {
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    if (item.containsKey("productId")) {
                        collected.add(item);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** 按 productId 去重并封顶到 MAX_PRODUCT_CARDS 件，保持首次出现顺序 */
    private List<JSONObject> dedupeAndLimitProducts(List<JSONObject> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        java.util.LinkedHashSet<Object> seen = new java.util.LinkedHashSet<>();
        List<JSONObject> deduped = new ArrayList<>();
        for (JSONObject p : products) {
            Object id = p.get("productId");
            if (id != null && seen.add(String.valueOf(id))) {
                deduped.add(p);
            }
        }
        return deduped.size() > MAX_PRODUCT_CARDS
                ? new ArrayList<>(deduped.subList(0, MAX_PRODUCT_CARDS))
                : deduped;
    }

    // ========== SSE event helpers ==========

    private void sendTextEvent(SseEmitter emitter, String text) {
        try {
            JSONObject event = new JSONObject();
            event.put("type", "text");
            event.put("content", text);
            emitter.send(SseEmitter.event().name("message").data(event.toJSONString()));
        } catch (Exception e) {
            log.debug("SSE send error", e);
        }
    }

    private void sendProductEvent(SseEmitter emitter, JSONObject product) {
        try {
            JSONObject event = new JSONObject();
            event.put("type", "product");
            event.put("productId", product.get("productId"));
            event.put("productName", product.getOrDefault("name", ""));
            event.put("productImage", product.getOrDefault("productImage", ""));
            event.put("price", product.getOrDefault("price", 0));
            event.put("score", product.getOrDefault("score", 0));
            event.put("reason", product.getOrDefault("reason", ""));
            event.put("tryOnSupported", false);
            emitter.send(SseEmitter.event().name("message").data(event.toJSONString()));
        } catch (Exception e) {
            log.debug("SSE product event error", e);
        }
    }

    private void sendDoneEvent(SseEmitter emitter, Long sessionId) {
        try {
            JSONObject event = new JSONObject();
            event.put("type", "done");
            event.put("sessionId", sessionId);
            emitter.send(SseEmitter.event().name("message").data(event.toJSONString()));
        } catch (Exception e) {
            log.debug("SSE done event error", e);
        }
    }

    private void sendErrorEvent(SseEmitter emitter, String error) {
        try {
            JSONObject event = new JSONObject();
            event.put("type", "error");
            event.put("content", error);
            emitter.send(SseEmitter.event().name("message").data(event.toJSONString()));
        } catch (Exception ex) {
            log.debug("SSE error event send failed", ex);
        }
    }

    // ========== Session & Message management ==========

    private ChatSession findOrCreateSession(Long sessionId, Long userId) {
        if (sessionId != null) {
            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session != null && session.getUserId().equals(userId)) {
                return session;
            }
        }

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle("新的对话");
        session.setStatus(1);
        chatSessionMapper.insert(session);
        return session;
    }

    private void generateTitleFromMessage(Long sessionId, String userMessage) {
        try {
            ChatResponse response = chatModel.chat(
                    SystemMessage.from("根据用户的提问生成一个简短的会话标题（不超过15字）。只返回标题文本，不加引号或标点。"),
                    UserMessage.from(userMessage)
            );
            String title = response.aiMessage().text();

            if (title != null && !title.isBlank()) {
                title = title.trim();
                if (title.length() > 50) title = title.substring(0, 50);
                chatSessionMapper.updateTitle(sessionId, title);
                log.info("Generated title for session {}: {}", sessionId, title);
                return;
            }
        } catch (Exception e) {
            log.warn("Failed to generate LLM title for session {}, using fallback", sessionId, e);
        }
        String fallback = userMessage.length() > 20 ? userMessage.substring(0, 20) + "..." : userMessage;
        chatSessionMapper.updateTitle(sessionId, fallback);
    }

    private void saveMessage(Long sessionId, String role, String content, String metadata) {
        com.recommendation.pojo.entity.ChatMessage message =
                new com.recommendation.pojo.entity.ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setMetadata(metadata);
        chatMessageMapper.insert(message);
    }

    // 构建每次 LLM 调用的消息上下文：system prompt + 最近 20 条历史
    private List<ChatMessage> buildLlmMessages(Long sessionId) {
        List<ChatMessage> messages = new ArrayList<>();

        messages.add(SystemMessage.from(productContextBuilder.buildSystemPrompt()));

        // 最近 20 条避免超过上下文窗口，同时保证足够的对话记忆
        List<com.recommendation.pojo.entity.ChatMessage> history =
                chatMessageMapper.selectBySessionId(sessionId);
        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            com.recommendation.pojo.entity.ChatMessage msg = history.get(i);
            switch (msg.getRole()) {
                case "user" -> messages.add(UserMessage.from(msg.getContent()));
                case "assistant" -> {
                    String content = msg.getContent();
                    String hint = buildProductIdHint(msg.getMetadata());
                    messages.add(AiMessage.from(hint.isEmpty() ? content : content + "\n" + hint));
                }
                default -> {}
            }
        }

        return messages;
    }

    /** 从助手消息 metadata 中提取商品 ID 提示，便于后续追问详情时调用 get_product_detail */
    private String buildProductIdHint(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return "";
        }
        try {
            JSONObject meta = JSON.parseObject(metadata);
            JSONArray products = meta.getJSONArray("products");
            if (products == null || products.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder("[以上推荐商品的ID:");
            for (int i = 0; i < products.size(); i++) {
                JSONObject p = products.getJSONObject(i);
                sb.append(" ").append(p.getString("name")).append("=").append(p.get("productId"));
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    // ========== Session list / history ==========

    @Override
    public List<ChatSessionVO> getSessions(Long userId) {
        List<ChatSession> sessions = chatSessionMapper.selectByUserId(userId);
        return sessions.stream().map(s -> {
            ChatSessionVO vo = new ChatSessionVO();
            vo.setId(s.getId());
            vo.setTitle(s.getTitle());
            vo.setUpdatedTime(s.getUpdatedTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageVO> getMessages(Long sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }

        List<com.recommendation.pojo.entity.ChatMessage> messages =
                chatMessageMapper.selectBySessionId(sessionId);
        return messages.stream().map(m -> {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(m.getId());
            vo.setRole(m.getRole());
            vo.setContent(m.getContent());
            vo.setMetadata(m.getMetadata());
            vo.setCreatedTime(m.getCreatedTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        chatSessionMapper.deleteById(sessionId);
    }
}
