package com.recommendation.server.mapper;

import com.recommendation.pojo.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatSessionMapper {

    ChatSession selectById(@Param("id") Long id);

    List<ChatSession> selectByUserId(@Param("userId") Long userId);

    int insert(ChatSession session);

    int updateTitle(@Param("id") Long id, @Param("title") String title);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int deleteById(@Param("id") Long id);
}
