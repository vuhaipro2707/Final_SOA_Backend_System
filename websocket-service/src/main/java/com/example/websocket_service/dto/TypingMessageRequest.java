package com.example.websocket_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingMessageRequest {
    private Long roomId;
    private int isTyping; // 1 cho TYPING, 0 cho STOP_TYPING
}