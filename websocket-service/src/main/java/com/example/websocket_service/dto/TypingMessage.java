package com.example.websocket_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingMessage {
    private Long roomId;
    private boolean isTyping; // true cho TYPING, false cho STOP_TYPING
    private Long customerId;
}