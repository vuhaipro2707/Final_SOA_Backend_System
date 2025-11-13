package com.example.chat_query_service.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreatedEvent {
    private Long roomId;
    private String roomName;
    private Long createdBy;
    private Instant createdAt;
    
    private List<ParticipantDTO> participants; 

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDTO {
        private Long id;
        private String fullName;
        private Instant joinedAt;
    }
}