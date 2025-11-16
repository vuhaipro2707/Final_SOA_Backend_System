package com.example.chat_command_service.dto;

import lombok.Data;

@Data
public class UpdateReadMarkerRequest {
    private Long roomId;
    private Long messageId;
}