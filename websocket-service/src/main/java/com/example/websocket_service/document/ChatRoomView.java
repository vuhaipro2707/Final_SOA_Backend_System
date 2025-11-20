package com.example.websocket_service.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomView {
    private Long roomId; 
    private String roomName;
    private List<Long> participantIds;
    private Map<Long, Boolean> unreadStatus;
    private LastMessageInfo lastMessage;
    private Long createdBy;
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