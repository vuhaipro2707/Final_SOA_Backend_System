package com.example.chat_query_service.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadStatusUpdateEvent {
    private Long roomId;
    private Long customerId;
    private Boolean isUnread;
}