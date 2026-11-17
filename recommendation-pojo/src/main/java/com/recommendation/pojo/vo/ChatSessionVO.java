package com.recommendation.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private LocalDateTime updatedTime;
}
