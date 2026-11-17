package com.recommendation.pojo.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String title;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
