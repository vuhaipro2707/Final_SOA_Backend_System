package com.example.chat_query_service.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSentEvent {
    private Long messageId; 
    private Long roomId;
    private Long senderId;
    private String senderFullName; 
    private String content;
    private Instant sentAt;
}