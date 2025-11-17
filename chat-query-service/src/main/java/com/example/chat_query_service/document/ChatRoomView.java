package com.example.chat_query_service.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "chatRoomViews")
@CompoundIndex(name = "user_rooms_sort_idx", def = "{'participantIds': 1, 'updatedAt': -1}")
public class ChatRoomView {

    @Id
    private Long roomId; 

    private String roomName;

    
    private List<Long> participantIds;

    private Map<Long, Boolean> unreadStatus;

    private LastMessageInfo lastMessage;

    private Instant createdAt;

    private Instant updatedAt;

    @Data
    public static class LastMessageInfo {
        private Long messageId;
        private Long senderId;
        private String content;
        private Instant sentAt;
    }
}