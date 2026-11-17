package com.recommendation.server.controller;

import com.recommendation.common.result.Result;
import com.recommendation.pojo.dto.ChatRequestDTO;
import com.recommendation.pojo.vo.ChatMessageVO;
import com.recommendation.pojo.vo.ChatSessionVO;
import com.recommendation.server.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "AI购物助手", description = "对话式商品推荐接口")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "发送消息(SSE流式响应)")
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(
            @RequestAttribute("userId") Long userId,
            @RequestBody ChatRequestDTO request) {
        return chatService.chat(request, userId);
    }

    @Operation(summary = "获取会话列表")
    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> getSessions(
            @RequestAttribute("userId") Long userId) {
        return Result.success(chatService.getSessions(userId));
    }

    @Operation(summary = "获取会话消息")
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessageVO>> getMessages(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long sessionId) {
        return Result.success(chatService.getMessages(sessionId, userId));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long sessionId) {
        chatService.deleteSession(sessionId, userId);
        return Result.success();
    }
}
