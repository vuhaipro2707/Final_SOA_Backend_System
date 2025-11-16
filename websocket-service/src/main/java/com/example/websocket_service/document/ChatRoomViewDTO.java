package com.example.websocket_service.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomViewDTO {
    private Long roomId; 
    private String roomName;
    private List<Long> participantIds;
    private Instant createdAt;
    private Instant updatedAt;
}