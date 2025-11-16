package com.example.websocket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOnlineStatus {
    private Long customerId;
    private boolean online; // true: CONNECT, false: DISCONNECT
}