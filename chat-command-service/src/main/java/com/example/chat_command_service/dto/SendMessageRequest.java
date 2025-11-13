package com.example.chat_command_service.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private Long roomId;
    private String content;
}