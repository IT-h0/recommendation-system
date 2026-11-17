package com.recommendation.pojo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;
    private Long sessionId;
}
