package com.recommendation.server.service;

import com.recommendation.pojo.dto.ChatRequestDTO;
import com.recommendation.pojo.vo.ChatMessageVO;
import com.recommendation.pojo.vo.ChatSessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {

    SseEmitter chat(ChatRequestDTO request, Long userId);

    List<ChatSessionVO> getSessions(Long userId);

    List<ChatMessageVO> getMessages(Long sessionId, Long userId);

    void deleteSession(Long sessionId, Long userId);
}
