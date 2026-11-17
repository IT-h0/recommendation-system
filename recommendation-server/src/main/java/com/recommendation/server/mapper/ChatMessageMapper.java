package com.recommendation.server.mapper;

import com.recommendation.pojo.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    List<ChatMessage> selectBySessionId(@Param("sessionId") Long sessionId);

    int insert(ChatMessage message);
}
