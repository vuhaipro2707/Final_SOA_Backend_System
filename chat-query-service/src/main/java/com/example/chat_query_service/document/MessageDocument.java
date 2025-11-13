package com.example.chat_query_service.document;

import lombok.Data;
import org.springframework.data.annotation.Id; // Giữ lại import này
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "messages")
@CompoundIndex(name = "room_message_idx", def = "{'roomId': 1, 'messageId': -1}")
public class MessageDocument {
    @Id 
    private Long messageId;

    private Long roomId;

    private Long senderId;
    
    private String senderFullName;

    private String content;

    private Instant sentAt; 
}