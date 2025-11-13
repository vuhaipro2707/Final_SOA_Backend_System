package com.example.chat_command_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateRoomRequest {
    private String roomName;
    private List<Long> targetCustomerIds;
}